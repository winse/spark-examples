package com.github.winse.spark;

import static com.sun.btrace.BTraceUtils.*;

import com.sun.btrace.annotations.*;

@BTrace
public class HelloWorldTrace {

    /*
com.sun.jndi.dns.DnsClient#query

com.sun.jndi.dns.DnsClient#doUdpQuery

sun.security.krb5.KrbServiceLocator#getKerberosService(java.lang.String)

sun.security.krb5.Config#checkRealm

org.apache.hadoop.security.authentication.util.KerberosUtil#getDefaultRealm
     */
    @Property
    private static long count;

    @OnMethod(clazz = "com.sun.jndi.dns.DnsClient", method = "query")
    public static void onQueryDns() {
        count++;
    }

    @OnTimer(2000)
    public static void ontimer() {
        println(count);
    }

}
