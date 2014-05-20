package pl.edu.agh.masters.cloudscrum.exception;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

public class Authorization extends Exception {

    UserRecoverableAuthIOException originalException;

    public Authorization(UserRecoverableAuthIOException exception) {
        originalException = exception;
    }

    public UserRecoverableAuthIOException getOriginalException() {
        return originalException;
    }
}
