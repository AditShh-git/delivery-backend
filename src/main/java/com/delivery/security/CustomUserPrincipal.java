package com.delivery.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserPrincipal implements UserDetails {

    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean onboarded;
    private final Long companyId;
    private final String zone;
    private final Boolean riderOnDuty; // null for non-RIDER users

    public CustomUserPrincipal(String email,
                               Collection<? extends GrantedAuthority> authorities,
                               boolean onboarded,
                               Long companyId,
                               String zone,
                               Boolean riderOnDuty) {
        this.email       = email;
        this.authorities = new ArrayList<>(authorities); // safe copy
        this.onboarded   = onboarded;
        this.companyId   = companyId;
        this.zone        = zone;
        this.riderOnDuty = riderOnDuty;
    }

    public boolean isOnboarded()  { return onboarded; }
    public Long getCompanyId()    { return companyId; }
    public String getZone()       { return zone; }
    /**
     * UI DISPLAY ONLY — snapshot captured at login time.
     * This value reflects isOnDuty at the moment the JWT was issued.
     * It becomes stale the instant an admin changes duty status between logins.
     *
     * ⚠️ NEVER use this for dispatch, availability checks, or any business logic.
     * Always call rider.isOnDuty() from the DB for those decisions.
     *
     * @return true if rider was on duty at login; null for non-RIDER users.
     */
    public Boolean getRiderOnDutySnapshot() { return riderOnDuty; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
