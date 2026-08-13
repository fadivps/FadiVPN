# FadiVPN Central Renewal

The application checks the central VMess configuration.

Rules:

1. The remote configuration must contain a valid VMess/VLESS URI.
2. `updated_at` identifies the remote revision.
3. A remote configuration is never installed before validation.
4. If validation fails, the currently working configuration remains active.
5. Renewal must never stop or restart the active VPN session.
6. A new configuration is used by the next VPN connection.
7. All application installations use the same central configuration.
