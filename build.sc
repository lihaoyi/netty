import mill._, javalib._
import $ivy.`org.codehaus.groovy:groovy:3.0.9`
import $ivy.`org.codehaus.groovy:groovy-ant:3.0.9`
import $ivy.`ant:ant-optional:1.5.3-1`
trait NettyModule extends MavenModule

// all/pom.xml
object all extends NettyModule{

}
// bom/pom.xml
object bom extends NettyModule{

}

// buffer/pom.xml
object buffer extends NettyModule{
  def moduleDeps = Seq(common)
//  def ivyDeps = Agg(ivy"org.mockito:mockito-core:???")
}


// codec/pom.xml
object codec extends NettyModule {
  def moduleDeps = Seq(common, buffer, transport)
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

// codec-dns/pom.xml
object `codec-dns` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-haproxy/pom.xml
object `codec-haproxy` extends NettyModule{
  def moduleDeps = Seq(buffer, transport, codec)
}

// codec-http/pom.xml
object `codec-http` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec, handler)
  def compileIvyDeps = Agg(
    ivy"com.jcraft:jzlib:1.1.3",
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
  )
}

// codec-http2/pom.xml
object `codec-http2` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec, handler, `codec-http`)
  def compileIvyDeps = Agg(
    ivy"com.aayushatharva.brotli4j:brotli4j:1.16.0",
  )
}

// codec-memcache/pom.xml
object `codec-memcache` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-mqtt/pom.xml
object `codec-mqtt` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-redis/pom.xml
object `codec-redis` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-smtp/pom.xml
object `codec-smtp` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-socks/pom.xml
object `codec-socks` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-stomp/pom.xml
object `codec-stomp` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
}

// codec-xml/pom.xml
object `codec-xml` extends NettyModule{
  def moduleDeps = Seq(buffer, transport, codec)
  def ivyDeps = Agg(
    ivy"com.fasterxml:aalto-xml:1.0.0"
  )
}

// common/pom.xml
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

// dev-tools/pom.xml
object `dev-tools` extends NettyModule{

}

// example/pom.xml
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


// handler/pom.xml
object handler extends NettyModule{
  def moduleDeps = Seq(common, resolver, buffer, transport, `transport-native-unix-common`, codec)

  def compileIvyDeps = Agg(
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
    ivy"org.conscrypt:conscrypt-openjdk-uber:2.5.2",
    ivy"io.netty:netty-tcnative-classes:2.0.65.Final",
    ivy"org.eclipse.jetty.alpn:alpn-api:1.1.2.v20150522",
    ivy"org.eclipse.jetty.npn:npn-api:1.1.1.v20141010",
  )

  def javacOptions = Seq("--add-exports", "java.base/sun.security.x509=ALL-UNNAMED")
}


// handler-proxy/pom.xml
object `handler-proxy` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec, `codec-socks`, `codec-http`, handler)
}

// handler-ssl-ocsp/pom.xml
object `handler-ssl-ocsp` extends NettyModule{
  def moduleDeps = Seq(`codec-http`, transport, `resolver-dns`)
  def ivyDeps = Agg(
    ivy"org.bouncycastle:bcpkix-jdk15on:1.69",
    ivy"org.bouncycastle:bctls-jdk15on:1.69",
  )
}
// microbench/pom.xml
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


// resolver/pom.xml
object resolver extends NettyModule{
  def moduleDeps = Seq(common)
}


// resolver-dns/pom.xml
object `resolver-dns` extends NettyModule{
  def moduleDeps = Seq(common, buffer, resolver, transport, codec, `codec-dns`, handler)
}

// resolver-dns-classes-macos/pom.xml
object `resolver-dns-classes-macos` extends NettyModule{
  def moduleDeps = Seq(common, resolver, `transport-native-unix-common`, `resolver-dns`)
}
// resolver-dns-native-macos/pom.xml
object `resolver-dns-native-macos` extends NettyModule{
  def moduleDeps = Seq(resolver)
}


// testsuite/pom.xml
object testsuite extends NettyModule{
  def moduleDeps = Seq(common, resolver, transport, `transport-sctp`, handler, `codec-http`, `transport-udt`)
  def ivyDeps = Agg(
    ivy"org.junit.jupiter:junit-jupiter-api:5.9.0",
    ivy"org.junit.jupiter:junit-jupiter-params:5.9.0",
    ivy"org.assertj:assertj-core:3.18.0",
    ivy"org.hamcrest:hamcrest-library:1.3",
    ivy"org.slf4j:slf4j-api:1.7.30",
    ivy"org.tukaani:xz:1.5",
  )
}

// testsuite-autobahn/pom.xml
object `testsuite-autobahn` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `codec-http`)
}

// testsuite-http2/pom.xml
object `testsuite-http2` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, handler, `codec-http`, `codec-http2`)
}


// testsuite-native/pom.xml
object `testsuite-native` extends NettyModule{
  def moduleDeps = Seq(`transport-native-kqueue`, `resolver-dns-native-macos`, `transport-native-epoll`)
}

// testsuite-native-image/pom.xml
object `testsuite-native-image` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, handler, `codec-http`)
}

// testsuite-native-image-client/pom.xml
object `testsuite-native-image-client` extends NettyModule{
  def moduleDeps = Seq(transport, `resolver-dns`)
}


// testsuite-native-image-client-runtime-init/pom.xml
object `testsuite-native-image-client-runtime-init` extends NettyModule{
  def moduleDeps = Seq(common)
}

// testsuite-osgi/pom.xml
object `testsuite-osgi` extends NettyModule{
  def moduleDeps = Seq(
    buffer,
    codec, `codec-dns`, `codec-haproxy`, `codec-http`, `codec-http2`, `codec-memcache`, `codec-mqtt`, `codec-socks`, `codec-stomp`,
    common,
    handler, `handler-proxy`,
    resolver, `resolver-dns`,
    transport, `transport-sctp`, `transport-udt`
  )
}
// testsuite-shading/pom.xml
object `testsuite-shading` extends NettyModule{

}


// transport/pom.xml
object transport extends NettyModule{
  def moduleDeps = Seq(common, buffer, resolver)
}

// transport-blockhound-tests/pom.xml
object `transport-blockhound-tests` extends NettyModule{
  def moduleDeps = Seq(transport, handler, `resolver-dns`)
}

// transport-classes-epoll/pom.xml
object `transport-classes-epoll` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`)
}

// transport-classes-kqueue/pom.xml
object `transport-classes-kqueue` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`)
}
// transport-native-epoll/pom.xml
object `transport-native-epoll` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`, `transport-classes-epoll`)
}
// transport-native-kqueue/pom.xml
object `transport-native-kqueue` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, `transport-native-unix-common`, `transport-classes-kqueue`)
}
// transport-native-unix-common/pom.xml
object `transport-native-unix-common` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport)
  def ivyDeps = Agg(
    ivy"org.junit.jupiter:junit-jupiter-api:5.9.0",
  )
}
// transport-native-unix-common-tests/pom.xml
object `transport-native-unix-common-tests` extends NettyModule{
  def moduleDeps = Seq(transport, `transport-native-unix-common`)
}
// transport-rxtx/pom.xml
object `transport-rxtx` extends NettyModule{
  def moduleDeps = Seq(buffer, transport)
  def ivyDeps = Agg(
    ivy"org.rxtx:rxtx:2.1.7"
  )
}
// transport-sctp/pom.xml
object `transport-sctp` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport, codec)
  def javacOptions = Seq(
    "-source", "1.8",
    "-target", "1.8"
  )
}
// transport-udt/pom.xml
object `transport-udt` extends NettyModule{
  def moduleDeps = Seq(common, buffer, transport)
  def ivyDeps = Agg(
    ivy"com.barchart.udt:barchart-udt-bundle:2.3.0"
  )
}
// pom.xml
