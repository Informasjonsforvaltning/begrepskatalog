package no.brreg.conceptcatalogue.security;

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class FdkPermissions{
    private fun authentication(): Authentication {
        return SecurityContextHolder.getContext().authentication
    }

    fun hasPermission(targetId: String?, targetType: String, permission: String): Boolean {
        val requiredAuthority: SimpleGrantedAuthority = SimpleGrantedAuthority("$targetType:$targetId:$permission");


        return authentication().authorities.contains(requiredAuthority);
    }
}