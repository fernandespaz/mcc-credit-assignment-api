package com.srm.mcc.credit.domain.enums;

/**
 * Roles that gate write/read access across the API.
 * <ul>
 *   <li>{@code ADMIN} — full access, including assignor PII and exchange rate management.</li>
 *   <li>{@code OPERATOR} — can register receivables and execute settlements (money movement).</li>
 *   <li>{@code VIEWER} — read-only access to all resources and reports.</li>
 * </ul>
 */
public enum UserRole {
    ADMIN,
    OPERATOR,
    VIEWER
}
