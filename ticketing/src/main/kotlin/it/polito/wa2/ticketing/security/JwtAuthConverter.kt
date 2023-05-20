package it.polito.wa2.ticketing.security


import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.stereotype.Component
import java.util.stream.Collectors
import java.util.stream.Stream

@Component
class JwtAuthConverter: Converter<Jwt, AbstractAuthenticationToken> {

    private val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()

    private var properties: JwtAuthConverterProperties? = null

    fun JwtAuthConverter(properties: JwtAuthConverterProperties?) {
        this.properties = properties
    }

    override fun convert(jwt: Jwt): AbstractAuthenticationToken? {
        println("CONVERTING")
        val authorities: Collection<GrantedAuthority?> = Stream.concat(
            jwtGrantedAuthoritiesConverter.convert(jwt)!!.stream(),
            extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet())
        return JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt))
    }

    private fun getPrincipalClaimName(jwt: Jwt): String? {
        println("Principal Claim Name")
        var claimName: String? = JwtClaimNames.SUB
        if (properties!!.principalAttribute != null) {
            claimName = properties!!.principalAttribute
        }
        return jwt.getClaim(claimName)
    }

    private fun extractResourceRoles(jwt: Jwt): Collection<GrantedAuthority?> {
        println("Extracting Resource Roles")
        val resourceAccess = jwt.getClaim<Map<String, Any>>("resource_access")
        val resource = resourceAccess?.get(properties?.resourceId) as Map<*, *>?
        val resourceRoles = resource?.get("roles") as Collection<*>?

        return if (resourceAccess == null || resource == null || resourceRoles == null) {
            println("null")
            setOf()
        } else {
            println("passed")
            resourceRoles.stream()
                .map { role -> SimpleGrantedAuthority("ROLE_$role") }
                .collect(Collectors.toSet())
        }
    }


}