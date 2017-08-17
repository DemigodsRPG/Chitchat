package com.demigodsrpg.chitchat

object Depends {
    val ORG_REDISSON = "org.redisson"
    val ORG_SLF4J = "org.slf4j"
    val COM_ESOTERICSOFTWARE = "com.esotericsoftware"
    val COM_FASTERXML_JACKSON_CORE = "com.fasterxml.jackson.core"

    val REDISSON = "redisson"
    val SLF4J_API = "slf4j-api"
    val KYRO = "kryo"
    val JACKSON_CORE = "jackson-core"
    val JACKSON_DATABIND = "jackson-databind"

    val REDISSON_VER = "\${redisson.version}"
    val SLF4J_API_VER = "\${slf4j.version}"
    val KYRO_VER = "\${kryo.version}"
    val JACKSON_VER = "\${jackson.version}"
}