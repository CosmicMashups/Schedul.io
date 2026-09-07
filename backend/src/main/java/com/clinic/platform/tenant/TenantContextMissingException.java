package com.clinic.platform.tenant;

import com.clinic.platform.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TenantContextMissingException extends ApiException {

    public TenantContextMissingException() {
        super("TENANT_CONTEXT_MISSING",
                "No tenant could be resolved for this request. Provide a valid X-Tenant-Id header or an authenticated session.",
                HttpStatus.BAD_REQUEST);
    }
}
