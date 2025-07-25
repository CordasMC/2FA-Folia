package com.lielamar.auth.shared.handlers;

import com.atlassian.onetime.core.TOTP;
import com.atlassian.onetime.model.TOTPSecret;
import com.atlassian.onetime.service.DefaultTOTPService;
import com.atlassian.onetime.service.RandomSecretProvider;
import com.atlassian.onetime.service.SecretProvider;
import com.lielamar.auth.bukkit.communication.BasicAuthCommunication;
import com.lielamar.auth.shared.communication.AuthCommunicationHandler;
import com.lielamar.auth.shared.storage.StorageHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AuthHandler {

    private final Map<UUID, String> pendingKeys;
    private final Map<UUID, Integer> failedAttempts;

    protected StorageHandler storageHandler;
    protected AuthCommunicationHandler authCommunicationHandler, fallbackCommunicationHandler;
    protected Map<UUID, AuthState> authStates;

    protected final DefaultTOTPService totpService;
    protected final SecretProvider secretProvider;

    public AuthHandler() {
        this(null, null);
    }

    public AuthHandler(@Nullable StorageHandler storageHandler, @Nullable AuthCommunicationHandler authCommunicationHandler) {
        this(storageHandler, authCommunicationHandler, null);
    }

    public AuthHandler(@Nullable StorageHandler storageHandler, @Nullable AuthCommunicationHandler authCommunicationHandler,
            @Nullable AuthCommunicationHandler fallbackCommunicationHandler) {
        this.pendingKeys = new ConcurrentHashMap<>();
        this.failedAttempts = new ConcurrentHashMap<>();
        this.secretProvider = new RandomSecretProvider();
        this.totpService = new DefaultTOTPService();

        this.storageHandler = storageHandler;
        this.authCommunicationHandler = authCommunicationHandler;
        this.fallbackCommunicationHandler = fallbackCommunicationHandler;

        this.authStates = new ConcurrentHashMap<>();
    }

    public @Nullable StorageHandler getStorageHandler() {
        return this.storageHandler;
    }

    public @Nullable AuthCommunicationHandler getAuthCommunicationHandler() {
        return this.authCommunicationHandler;
    }

    /**
     * Returns a player's key
     *
     * @param uuid UUID of the player to get the key of
     * @return Player's key
     */
    protected @Nullable String getKey(@NotNull UUID uuid) {
        if (this.getStorageHandler() == null) {
            return null;
        }

        if (!this.is2FAEnabled(uuid)) {
            return null;
        }

        return this.getStorageHandler().getKey(uuid);
    }

    /**
     * Returns a player's pending key (if they have one)
     *
     * @param uuid UUID of the player to get the pending key of
     * @return Player's pending key
     */
    protected @Nullable String getPendingKey(@NotNull UUID uuid) {
        if (!this.isPendingSetup(uuid)) {
            return null;
        }

        return this.pendingKeys.get(uuid);
    }

    /**
     * Returns a Player's Auth State
     *
     * @param uuid UUID of the player to get the Auth State of
     * @return Auth State
     */
    public @NotNull AuthState getAuthState(@NotNull UUID uuid) {
        if (this.authStates.containsKey(uuid)) {
            return this.authStates.get(uuid);
        }

        return AuthState.NONE;
    }

    /**
     * Checks if a player has a Secret Key
     *
     * @param uuid UUID of the player to check
     * @return Whether or not the player has a Secret Key
     */
    public boolean is2FAEnabled(@NotNull UUID uuid) {
        AuthState state = this.authStates.get(uuid);
        if (state == null) {
            return false;
        }

        return state.equals(AuthState.DEMAND_SETUP)
                || state.equals(AuthState.PENDING_LOGIN) || state.equals(AuthState.AUTHENTICATED);
    }

    /**
     * Checks if a player is Pending 2FA Setup
     *
     * @param uuid UUID of the player to check
     * @return Whether or not the player is pending setup
     */
    public boolean isPendingSetup(@NotNull UUID uuid) {
        AuthState state = this.authStates.get(uuid);
        if (state == null) {
            return false;
        }
        return state.equals(AuthState.PENDING_SETUP) || state.equals(AuthState.DEMAND_SETUP);
    }

    /**
     * Creates a Key for a player
     *
     * @param uuid UUID of the player to create the Secret Key for
     * @return Created Key
     */
    public @NotNull String createKey(@NotNull UUID uuid) {
        TOTPSecret key = secretProvider.generateSecret();

        this.changeState(uuid, AuthState.PENDING_SETUP);
        this.pendingKeys.put(uuid, key.getBase32Encoded());
        return key.getBase32Encoded();
    }

    /**
     * Validates a code with the player's key
     *
     * @param uuid UUID of the player to validate the key of
     * @param code Inserted code
     * @return Whether the code is valid
     */
    public boolean validateKey(@NotNull UUID uuid, @NotNull String code) {
        String base32Key = this.getKey(uuid);
        AuthState state = this.authStates.get(uuid);

        if (base32Key != null && state != null && totpService.verify(new TOTP(code), TOTPSecret.Companion.fromBase32EncodedString(base32Key)).isSuccess()
                && state.equals(AuthState.PENDING_LOGIN)) {
            this.changeState(uuid, AuthState.AUTHENTICATED);
            return true;
        }

        return false;
    }

    /**
     * Approves a key and storing it
     *
     * @param uuid UUID of the player to validate the key of
     * @param code Inserted code
     * @return Whether the code is valid
     */
    public boolean approveKey(@NotNull UUID uuid, @NotNull String code) {
        if (this.getStorageHandler() == null) {
            return false;
        }

        String base32Key = this.getPendingKey(uuid);
        AuthState state = this.authStates.get(uuid);

        if (base32Key != null && state != null && totpService.verify(new TOTP(code), TOTPSecret.Companion.fromBase32EncodedString(base32Key)).isSuccess()
                && (state.equals(AuthState.PENDING_SETUP) || state.equals(AuthState.DEMAND_SETUP))) {
            this.changeState(uuid, AuthState.AUTHENTICATED);

            this.getStorageHandler().setKey(uuid, base32Key);
            this.getStorageHandler().setEnableDate(uuid, System.currentTimeMillis());
            this.pendingKeys.remove(uuid);

            return true;
        }
        return false;
    }

    /**
     * Resets a player's key
     *
     * @param uuid UUID of the player to reset the key of
     */
    public void resetKey(@NotNull UUID uuid) {
        if (this.getStorageHandler() == null) {
            return;
        }

        this.changeState(uuid, AuthState.DISABLED);

        this.getStorageHandler().removeKey(uuid);
        this.getStorageHandler().setEnableDate(uuid, -1);

        this.pendingKeys.remove(uuid);
    }

    /**
     * Cancels a player's key
     *
     * @param uuid UUID of the player to reset the key of
     * @return Whether cancellation was successful
     */
    public boolean cancelKey(@NotNull UUID uuid) {
        String key = getPendingKey(uuid);
        AuthState state = this.authStates.get(uuid);

        if (key != null && state != null && (state.equals(AuthState.PENDING_SETUP)
                || state.equals(AuthState.DEMAND_SETUP))) {
            this.changeState(uuid, AuthState.DISABLED);

            this.pendingKeys.remove(uuid);
            return true;
        }

        return false;
    }

    /**
     * Checks if a player needs to authenticate
     *
     * @param uuid UUID of the player to check
     * @return Whether or not the player needs to authenticate
     */
    public boolean needsToAuthenticate(@NotNull UUID uuid) {
        if (!this.is2FAEnabled(uuid)) {
            return false;
        }
        
        AuthState state = this.authStates.get(uuid);
        return state != null && !state.equals(AuthState.AUTHENTICATED);
    }

    /**
     * Returns the amount of times a player has failed authentication
     *
     * @param uuid UUID of the player to check
     * @param amount
     * @return Amount of fails
     */
    public int increaseFailedAttempts(@NotNull UUID uuid, int amount) {
        if (!this.failedAttempts.containsKey(uuid)) {
            this.failedAttempts.put(uuid, amount);

            return amount;
        } else {
            int playerFailedAttempts = this.failedAttempts.get(uuid) + amount;
            this.failedAttempts.put(uuid, playerFailedAttempts);

            return playerFailedAttempts;
        }
    }

    public void playerQuit(@NotNull UUID uuid) {
        pendingKeys.remove(uuid);
        authStates.remove(uuid);
        if (authCommunicationHandler instanceof BasicAuthCommunication authCommunication) {
            authCommunication.handleQuit(uuid);
        }
    }

    public abstract void changeState(@NotNull UUID uuid, @NotNull AuthState authState);

    public enum AuthState {
        NONE, DISABLED, PENDING_SETUP, DEMAND_SETUP, PENDING_LOGIN, AUTHENTICATED
    }
}
