package no.brreg.conceptcatalogue.service.permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceRoleFactory {
    private static Logger logger = LoggerFactory.getLogger(ResourceRoleFactory.class);

    public static ResourceRole deserialize(String roleToken) {
        try {
            String[] parts = roleToken.split(":");
            String resourceType = parts[0];
            String resourceId = parts[1];
            String resourceRole = parts[2];

            if (OrganizationResourceRole.resourceType.equals(resourceType)) {
                return new OrganizationResourceRole(resourceId, OrganizationResourceRole.OrganizationRole.valueOf(resourceRole));
            }
            throw new IllegalArgumentException("Unknown resoureceType");
        } catch (Exception e) {
            logger.warn("Error parsing ResourceRole token", e);
            return null;
        }
    }

}
