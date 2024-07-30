import mill._, javalib._
import $ivy.`org.codehaus.groovy:groovy:3.0.9`
import $ivy.`org.codehaus.groovy:groovy-ant:3.0.9`
import $ivy.`ant:ant-optional:1.5.3-1`
// TODO:
// testsuite*
// transport-native-epoll.test
//   failed to load the required native library
// transport-udt

trait NettyBaseModule extends MavenModule{
  def javacOptions = Seq("-source", "1.8", "-target", "1.8")
}
trait NettyBaseTestSuiteModule extends NettyBaseModule with TestModule.Junit5{

  def testFramework = "com.github.sbt.junit.jupiter.api.JupiterFramework"
  def ivyDeps = Agg(
    ivy"com.github.sbt.junit:jupiter-interface:0.11.2",
    ivy"org.hamcrest:hamcrest-library:1.3",
    ivy"org.assertj:assertj-core:3.18.0",
    ivy"org.junit.jupiter:junit-jupiter-api:5.9.0",
    ivy"org.junit.jupiter:junit-jupiter-params:5.9.0",
    ivy"org.mockito:mockito-core:2.18.3",
    ivy"org.reflections:reflections:0.10.2",
    ivy"com.google.code.gson:gson:2.8.9",
    ivy"com.google.guava:guava:28.2-jre",
    ivy"org.jctools:jctools-core:4.0.5",
    ivy"io.netty:netty-tcnative-classes:2.0.65.Final",
    ivy"io.netty:netty-tcnative-boringssl-static:2.0.65.Final",
    ivy"com.barchart.udt:barchart-udt-bundle:2.3.0",
    ivy"com.aayushatharva.brotli4j:native-osx-aarch64:1.16.0",
    ivy"org.jboss.marshalling:jboss-marshalling:2.0.5.Final",
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
    ivy"org.apache.commons:commons-compress:1.26.0",
    ivy"com.jcraft:jzlib:1.1.3",
    ivy"net.jpountz.lz4:lz4:1.3.0",
    ivy"com.ning:compress-lzf:1.0.3",
    ivy"com.github.jponge:lzma-java:1.3",
    ivy"com.github.luben:zstd-jni:1.5.5-11",
    ivy"ch.qos.logback:logback-classic:1.1.7",
    ivy"org.eclipse.jetty.npn:npn-api:1.1.1.v20141010",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
  )

  def forkArgs = Seq(
    "-DnativeImage.handlerMetadataGroupId=io.netty",
    "-Dio.netty.bootstrap.extensions=serviceload",
    "-XX:+AllowRedefinitionToAddDeleteMethods",
    "--add-exports", "java.base/sun.security.x509=ALL-UNNAMED",
    "-enableassertions"
  )

  def compile = T{
    // Hack to satisfy fragile tests that look for /test-classes/ in the file paths
    val sup = super.compile()
    val testClasses = T.dest / "test-classes"
    if (os.exists(sup.classes.path)) os.copy(sup.classes.path, testClasses, createFolders = true)
    sup.copy(classes = PathRef(testClasses))
  }
}
trait NettyTestSuiteModule extends NettyBaseTestSuiteModule{

}
trait NettyModule extends NettyBaseModule{
  def testModuleDeps: Seq[MavenModule] = Nil
  def testIvyDeps: T[Agg[mill.scalalib.Dep]] = T{ Agg() }

  object test extends NettyBaseTestSuiteModule with MavenTests{
    def moduleDeps = super.moduleDeps ++ testModuleDeps
    def ivyDeps = super.ivyDeps() ++ testIvyDeps()
    def forkWorkingDir = NettyModule.this.millSourcePath
    def forkArgs = super.forkArgs() ++ Seq(
      "-Dnativeimage.handlerMetadataArtifactId=netty-" + NettyModule.this.millModuleSegments.parts.last,
    )

  }
}

trait NettyJniModule extends NettyModule {
  def jniLibraryName: T[String]
  def cSources = T.source(millSourcePath / "src" / "main" / "c")
  def resources = T{
    os.copy(clang().path, T.dest / "META-INF" / "native" / jniLibraryName(), createFolders = true)
    Seq(PathRef(T.dest))
  }
  def clang = T{
    val Seq(sourceJar) = resolveDeps(
      deps = T.task(Agg(ivy"io.netty:netty-jni-util:0.0.9.Final").map(bindDependency())),
      sources = true
    )().toSeq

    os.makeDir.all(T.dest  / "src" / "main" / "c")
    os.proc("jar", "xf", sourceJar.path).call(cwd = T.dest  / "src" / "main" / "c")

    os.proc(
      "clang",
      // CFLAGS
      "-O3", "-Werror", "-fno-omit-frame-pointer",
      "-Wunused-variable", "-fvisibility=hidden",
      "-I" + (T.dest / "src" / "main" / "c"),
      "-I" + `transport-native-unix-common`.cHeaders().path,
      "-I" + sys.props("java.home") + "/include/",
      "-I" + sys.props("java.home") + "/include/darwin",
      // LD_FLAGS
      "-Wl,-weak_library," + (`transport-native-unix-common`.make()._1.path / "libnetty-unix-common.a"),
      "-Wl,-platform_version,macos,10.9,10.9",
      "-Wl,-single_module",
      "-Wl,-undefined",
      "-Wl,dynamic_lookup",
      "-fno-common",
      "-DPIC",
      // sources
      os.list(cSources().path)
    ).call(cwd = T.dest, env = Map("MACOSX_DEPLOYMENT_TARGET" -> "10.9"))

    PathRef(T.dest / "a.out")

  }
}


object all extends NettyModule{

}

object bom extends NettyModule{

}

object buffer extends NettyModule{
  def moduleDeps = Seq(common)
  def testIvyDeps = Agg(ivy"org.jctools:jctools-core:4.0.5")
}

object codec extends NettyModule {
  def moduleDeps = Seq(common, buffer, transport)
  def testModuleDeps = Seq(transport.test)
  def ivyDeps = Agg(
    ivy"com.google.protobuf:protobuf-java:2.6.1",
  )
  def compileIvyDeps = Agg(
    ivy"org.jboss.marshalling:jboss-marshalling:2.0.5.Final",
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
    ivy"com.jcraft:jzlib:1.1.3",
    ivy"net.jpountz.lz4:lz4:1.3.0",
    ivy"com.ning:compress-lzf:1.0.3",
    ivy"com.github.jponge:lzma-java:1.3",
    ivy"com.github.luben:zstd-jni:1.5.5-11",
    ivy"com.google.protobuf.nano:protobuf-javanano:3.0.0-alpha-5",
  )
}

object `codec-dns` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-haproxy` extends NettyModule{
  def moduleDeps = Seq(buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-http` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec, handler)
  def testModuleDeps = Seq(transport.test)
  def compileIvyDeps = Agg(
    ivy"com.jcraft:jzlib:1.1.3",
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
  )
  def testIvyDeps = Agg(
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
  )
}

object `codec-http2` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec, handler, `codec-http`)
  def testModuleDeps = Seq(transport.test)
  def compileIvyDeps = Agg(
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
  )
}

object `codec-memcache` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-mqtt` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-redis` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-smtp` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-socks` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-stomp` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `codec-xml` extends NettyModule{
  def moduleDeps = Seq(buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
  def ivyDeps = Agg(
    ivy"com.fasterxml:aalto-xml:1.0.0"
  )
}

object common extends NettyModule{
  def compileIvyDeps = Agg(
    ivy"org.jctools:jctools-core:4.0.5",
    ivy"org.graalvm.nativeimage:svm:19.3.6",
    ivy"org.jetbrains:annotations-java5:23.0.0",
    ivy"io.projectreactor.tools:blockhound:1.0.6.RELEASE",
    ivy"commons-logging:commons-logging:1.2",
    ivy"org.apache.logging.log4j:log4j-api:2.17.2",
    ivy"org.apache.logging.log4j:log4j-1.2-api:2.17.2",
    ivy"org.slf4j:slf4j-api:1.7.30",
  )
  def testIvyDeps = Agg(
    ivy"org.jetbrains:annotations-java5:23.0.0",
    ivy"commons-logging:commons-logging:1.2",
    ivy"org.apache.logging.log4j:log4j-api:2.17.2",
    ivy"org.apache.logging.log4j:log4j-core:2.17.2",
    ivy"org.apache.logging.log4j:log4j-1.2-api:2.17.2",
    ivy"org.jctools:jctools-core:4.0.5",
  )

  def script = T.source(millSourcePath / "src" / "main" / "script")
  def generatedSources0 = T{
    val shell = new groovy.lang.GroovyShell()

    val context = new java.util.HashMap[String, Object]
    context.put("collection.template.dir", "common/src/main/templates")
    context.put("collection.template.test.dir", "common/src/test/templates")
    context.put("collection.src.dir", (T.dest / "src").toString)
    context.put("collection.testsrc.dir", (T.dest / "testsrc").toString)
    shell.setProperty("properties", context)
    shell.setProperty("ant", new groovy.ant.AntBuilder())
    shell.evaluate((script().path / "codegen.groovy").toIO)
    (PathRef(T.dest / "src"), PathRef(T.dest / "testsrc"))
  }

  def generatedSources = T{ Seq(generatedSources0()._1)}
}

object `dev-tools` extends NettyModule{

}

object example extends NettyModule{
  def ivyDeps = Agg(
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
    ivy"com.sun.activation:javax.activation:1.2.0"

  )
  def moduleDeps = Seq(
    common,
    buffer,
    transport,
    codec,
    handler,
    `transport-sctp`, `transport-rxtx`, `transport-udt`,
    `handler-proxy`, `codec-http`, `codec-memcache`,
    `codec-http2`, `codec-redis`, `codec-socks`, `codec-stomp`, `codec-mqtt`, `codec-haproxy`, `codec-dns`
  )
}

object handler extends NettyModule{
  def moduleDeps = Seq(common, resolver, buffer, transport, `transport-native-unix-common`, codec)
  def testModuleDeps = Seq(transport.test)
  def compileIvyDeps = Agg(
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
    ivy"org.conscrypt:conscrypt-openjdk-uber:2.5.2",
    ivy"io.netty:netty-tcnative-classes:2.0.65.Final",
    ivy"org.eclipse.jetty.alpn:alpn-api:1.1.2.v20150522",
    ivy"org.eclipse.jetty.npn:npn-api:1.1.1.v20141010",
  )

  def testIvyDeps = Agg(
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
    ivy"software.amazon.cryptools:AmazonCorrettoCryptoProvider:1.1.0;classifier=linux-x86_64",
    ivy"org.conscrypt:conscrypt-openjdk-uber:2.5.2",

  )
}

object `handler-proxy` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec, `codec-socks`, `codec-http`, handler)
  def testModuleDeps = Seq(transport.test)
}

object `handler-ssl-ocsp` extends NettyModule{
  def moduleDeps = Seq(`codec-http`, transport, `resolver-dns`)
  def ivyDeps = Agg(
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
  )
}

object microbench extends NettyModule{

  def moduleDeps = Seq(
    handler, `codec-http`, `codec-http2`, `codec-redis`, `codec-mqtt`, `codec-stomp`,
    `transport-native-epoll`, `transport-native-kqueue`
  )

  def ivyDeps = Agg(
    ivy"org.junit.jupiter:junit-jupiter-api:5.9.0",
    ivy"org.jctools:jctools-core:4.0.5",
    ivy"org.openjdk.jmh:jmh-core:1.36",
    ivy"org.openjdk.jmh:jmh-generator-annprocess:1.36",
    ivy"org.agrona:Agrona:0.5.1"
  )
}

object resolver extends NettyModule{
  def moduleDeps = Seq(common)
}

object `resolver-dns` extends NettyModule{
  def moduleDeps = Seq(common, buffer, resolver, transport, codec, `codec-dns`, handler)
  def testModuleDeps = Seq(transport.test)
  def testIvyDeps = Agg(
    ivy"org.apache.directory.server:apacheds-protocol-dns:1.5.7"
  )
}

object `resolver-dns-classes-macos` extends NettyModule{
  def moduleDeps = Seq(common, resolver, `transport-native-unix-common`, `resolver-dns`)
}


object `resolver-dns-native-macos` extends NettyJniModule {
  def jniLibraryName = "libnetty_resolver_dns_native_macos_aarch_64.jnilib"
  def moduleDeps = Seq(resolver)
  def testModuleDeps = Seq(`resolver-dns`, `resolver-dns-classes-macos`)
  def testIvyDeps = Agg(
    ivy"org.apache.directory.server:apacheds-protocol-dns:1.5.7"
  )


}

object testsuite extends NettyTestSuiteModule{
  def moduleDeps = Seq(common, resolver, transport, `transport-sctp`, handler, `codec-http`, `transport-udt`)
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.slf4j:slf4j-api:1.7.30",
    ivy"org.tukaani:xz:1.5",
  )
}


object `testsuite-autobahn` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `codec-http`)
}


object `testsuite-http2` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, handler, `codec-http`, `codec-http2`)
}

object `testsuite-native` extends NettyModule{
  def moduleDeps = Seq(`transport-native-kqueue`, `resolver-dns-native-macos`, `transport-native-epoll`)
  def testModuleDeps = Seq(`resolver-dns-classes-macos`)
}

object `testsuite-native-image` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, handler, `codec-http`)
}

object `testsuite-native-image-client` extends NettyModule{
  def moduleDeps = Seq(transport, `resolver-dns`)
}

object `testsuite-native-image-client-runtime-init` extends NettyModule{
  def moduleDeps = Seq(common)
}


object `testsuite-osgi` extends NettyModule{
  def moduleDeps = Seq(
    buffer,
    codec, `codec-dns`, `codec-haproxy`, `codec-http`, `codec-http2`, `codec-memcache`, `codec-mqtt`, `codec-socks`, `codec-stomp`,
    common,
    handler, `handler-proxy`,
    resolver, `resolver-dns`,
    transport, `transport-sctp`, `transport-udt`
  )

  def ivyDeps = Agg(
    ivy"org.apache.felix:org.apache.felix.configadmin:1.9.14",
  )
  def testIvyDeps = Agg(
    ivy"org.ops4j.pax.exam:pax-exam-junit4:4.13.0",
    ivy"org.ops4j.pax.exam:pax-exam-container-native:4.13.0",
    ivy"org.ops4j.pax.exam:pax-exam-link-assembly:4.13.0",
    ivy"org.apache.felix:org.apache.felix.framework:6.0.2",
  )
}

object `testsuite-shading` extends NettyModule{
  def moduleDeps = Seq(common)
}

object transport extends NettyModule{
  def moduleDeps = Seq(common, buffer, resolver)
}

object `transport-blockhound-tests` extends NettyModule{
  def moduleDeps = Seq(transport, handler, `resolver-dns`)
  def ivyDeps = Agg(
    ivy"io.projectreactor.tools:blockhound:1.0.6.RELEASE"
  )
}

object `transport-classes-epoll` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`)
}

object `transport-classes-kqueue` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`)
}

object `transport-native-epoll` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`, `transport-classes-epoll`)
  def testModuleDeps = Seq(testsuite, `transport-native-unix-common-tests`)

  def testIvyDeps = Agg(
    ivy"io.github.artsok:rerunner-jupiter:2.1.6"
  )
}

object `transport-native-kqueue` extends NettyJniModule{
  def jniLibraryName = "libnetty_transport_native_kqueue.jnilib"
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`, `transport-classes-kqueue`)
  def testModuleDeps = Seq(testsuite, `transport-native-unix-common-tests`)
}

object `transport-native-unix-common` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport)
  def ivyDeps = Agg(ivy"org.junit.jupiter:junit-jupiter-api:5.9.0")

  def makefile = T.source(millSourcePath / "Makefile")
  def cSources = T.source(millSourcePath / "src" / "main" / "c")
  def cHeaders = T{
    for(p <- os.walk(cSources().path) if p.ext == "h"){
      os.copy(p, T.dest / p.relativeTo(cSources().path), createFolders = true)
    }
    PathRef(T.dest)
  }

  def make = T{
    val Seq(sourceJar) = resolveDeps(
      deps = T.task(Agg(ivy"io.netty:netty-jni-util:0.0.9.Final").map(bindDependency())),
      sources = true
    )().toSeq

    os.copy(makefile().path, T.dest / "Makefile")
    os.copy(cSources().path, T.dest / "src" / "main" / "c", createFolders = true)
    os.proc("jar", "xf", sourceJar.path).call(cwd = T.dest  / "src" / "main" / "c")

    os.proc("make").call(
      cwd = T.dest,
      env = Map(
        "CC" -> "clang",
        "AR" -> "ar",
        "JNI_PLATFORM" -> "darwin",
        "LIB_DIR" -> "lib-out",
        "OBJ_DIR" -> "obj-out",
        "MACOSX_DEPLOYMENT_TARGET" -> "10.9",
        "CFLAGS" -> Seq(
          "-mmacosx-version-min=10.9",
          "-O3",
          "-Werror",
          "-Wno-attributes",
          "-fPIC",
          "-fno-omit-frame-pointer",
          "-Wunused-variable",
          "-fvisibility=hidden",
          "-I" + sys.props("java.home") + "/include/",
          "-I" + sys.props("java.home") + "/include/darwin",
        ).mkString(" "),
        "LD_FLAGS" -> "-Wl,--no-as-needed -lrt -Wl,-platform_version,macos,10.9,10.9",
        "LIB_NAME" -> "libnetty-unix-common"
      )
    )


    (PathRef(T.dest / "lib-out"), PathRef(T.dest / "obj-out"))
  }
}

object `transport-native-unix-common-tests` extends NettyModule{
  def moduleDeps = Seq(transport, `transport-native-unix-common`)
}

object `transport-rxtx` extends NettyModule{
  def moduleDeps = Seq(buffer, transport)
  def ivyDeps = Agg(
    ivy"org.rxtx:rxtx:2.1.7"
  )
}

object `transport-sctp` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def testModuleDeps = Seq(transport.test)
}

object `transport-udt` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport)
  def ivyDeps = Agg(
    ivy"com.barchart.udt:barchart-udt-bundle:2.3.0",
    ivy"com.google.caliper:caliper:0.5-rc1",
    ivy"com.yammer.metrics:metrics-core:2.2.0"
  )
}

