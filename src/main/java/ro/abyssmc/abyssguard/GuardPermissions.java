package ro.abyssmc.abyssguard;

import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.*;
import java.util.UUID;

/** Optional LuckPerms integration. Fail closed if LuckPerms isn't installed/loaded. */
public final class GuardPermissions {
    private GuardPermissions(){}
    public static boolean has(ServerPlayer player,String node){
        try {
            Class<?> provider=Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api=provider.getMethod("get").invoke(null);
            Object manager=api.getClass().getMethod("getUserManager").invoke(api);
            Object user=manager.getClass().getMethod("getUser",UUID.class).invoke(manager,player.getUUID());
            if(user==null)return false;
            Object cached=user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData=cached.getClass().getMethod("getPermissionData").invoke(cached);
            Object tristate=permissionData.getClass().getMethod("checkPermission",String.class).invoke(permissionData,node);
            return "TRUE".equals(String.valueOf(tristate));
        } catch(ReflectiveOperationException|LinkageError e){return false;}
    }
}
