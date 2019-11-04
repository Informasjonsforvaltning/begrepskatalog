package no.brreg.conceptcatalogue.service.permission

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class PermissionService {
    private fun authentication(): Authentication {
        return SecurityContextHolder.getContext().authentication
    }

    fun hasPermission(targetType: String, targetId: String? = "", permission: String): Boolean {
        val requiredAuthority: SimpleGrantedAuthority = SimpleGrantedAuthority("$targetType:$targetId:$permission");


        return authentication().authorities.contains(requiredAuthority);
    }
}