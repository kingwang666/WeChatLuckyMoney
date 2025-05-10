package xyz.monkeytong.hongbao.permisson;

import androidx.annotation.NonNull;

/**
 * Created by wangxiaojie on 2025/3/24.
 */
public final class PermissionResult {

    @NonNull
    public final String[] grantedPermissions;

    @NonNull
    public final String[] deniedPermissions;

    public PermissionResult() {
        this(new String[0], new String[0]);
    }

    public PermissionResult(@NonNull String[] grantedPermissions, @NonNull String[] deniedPermissions) {
        this.grantedPermissions = grantedPermissions;
        this.deniedPermissions = deniedPermissions;
    }

    public boolean hasAllPermissions() {
        return deniedPermissions.length == 0;
    }
}
