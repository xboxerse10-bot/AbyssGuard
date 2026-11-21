package ro.abyssmc.abyssguard;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import java.nio.file.Path;
import java.util.*;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

/** Alpha integration. See README for limitations and unverified 26.3 API bindings. */
public final class AbyssGuard implements ModInitializer {
    public static final RegionManager REGIONS=new RegionManager();
    public static final Selection SELECTIONS=new Selection();
    private static final RegionStore STORE=new RegionStore(Path.of("config","abyssguard","regions.json"));
    @Override public void onInitialize() {
        try { STORE.load(REGIONS); } catch(Exception e) { throw new IllegalStateException("AbyssGuard cannot load region data; refusing to start",e); }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var rg = literal("rg");
            rg.then(literal("pos1").executes(ctx -> setPos(ctx.getSource(), 1)));
            rg.then(literal("pos2").executes(ctx -> setPos(ctx.getSource(), 2)));
            rg.then(literal("define").then(argument("id", StringArgumentType.word())
                .executes(ctx -> define(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            rg.then(literal("delete").then(argument("id", StringArgumentType.word())
                .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            rg.then(literal("info").then(argument("id", StringArgumentType.word())
                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            rg.then(literal("list").executes(ctx -> {
                message(ctx.getSource(), "Regions: " + REGIONS.all().stream().map(r -> r.id).toList());
                return 1;
            }));
            rg.then(literal("flag").then(argument("id", StringArgumentType.word())
                .then(argument("flag", StringArgumentType.word())
                .then(argument("value", StringArgumentType.greedyString())
                .executes(ctx -> flag(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "flag"), StringArgumentType.getString(ctx, "value")))))));
            rg.then(literal("priority").then(argument("id", StringArgumentType.word())
                .then(argument("value", IntegerArgumentType.integer())
                .executes(ctx -> priority(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    IntegerArgumentType.getInteger(ctx, "value"))))));
            rg.then(literal("addmember").then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), false, false)))));
            rg.then(literal("removemember").then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), false, true)))));
            rg.then(literal("addowner").then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), true, false)))));
            rg.then(literal("removeowner").then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), true, true)))));
            dispatcher.register(rg);
        });
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->permitted(player,pos,Flag.BLOCK_BREAK));
        AttackBlockCallback.EVENT.register((player,world,hand,pos,direction)->permitted(player,pos,Flag.BLOCK_BREAK) ? InteractionResult.PASS : InteractionResult.FAIL);
        UseBlockCallback.EVENT.register((player,world,hand,hit)->permitted(player,hit.getBlockPos(),Flag.INTERACT) ? InteractionResult.PASS : InteractionResult.FAIL);
    }
    private static boolean permitted(net.minecraft.world.entity.player.Player player,BlockPos pos,Flag flag) {
        if (!(player instanceof ServerPlayer p)) return true;
        return REGIONS.allowed(p.level().dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),p.getUUID(),flag,false);
    }
    private static void message(CommandSourceStack src,String text) { src.sendSuccess(()->Component.literal("[AbyssGuard] "+text),false); }
    private static ServerPlayer player(CommandSourceStack src) { return src.getEntity() instanceof ServerPlayer p ? p : null; }
    private static String dim(ServerPlayer p) { return p.level().dimension().identifier().toString(); }
    private static int setPos(CommandSourceStack src,int slot) {
        ServerPlayer p=player(src); if(p==null) return 0;
        BlockPos pos=p.blockPosition(); SELECTIONS.set(p.getUUID(),slot,new Selection.Point(dim(p),pos.getX(),pos.getY(),pos.getZ()));
        message(src,"Position "+slot+" set: "+pos); return 1;
    }
    private static int define(CommandSourceStack src,String id) {
        ServerPlayer p=player(src); if(p==null) return 0;
        Selection.Point[] points=SELECTIONS.get(p.getUUID());
        if(points[0]==null||points[1]==null||!points[0].dimension().equals(points[1].dimension())) { message(src,"Set /rg pos1 and /rg pos2 in the same dimension first"); return 0; }
        try {
            Region r=new Region(id,points[0].dimension(),points[0].x(),points[0].y(),points[0].z(),points[1].x(),points[1].y(),points[1].z(),p.getUUID());
            // Prevent claiming another owner's territory in the alpha release.
            for(Region other:REGIONS.all()) if(other.intersects(r) && !other.owners.contains(p.getUUID())) { message(src,"Overlaps a region you do not own"); return 0; }
            REGIONS.add(r); save(); message(src,"Created region "+r.id); return 1;
        } catch(Exception e) { message(src,"Error: "+e.getMessage()); return 0; }
    }
    private static Region editable(CommandSourceStack src,String id) {
        Region r=REGIONS.get(id); ServerPlayer p=player(src);
        if(r==null || (p!=null && !r.owners.contains(p.getUUID()))) { message(src,"Region not found or you are not an owner"); return null; }
        return r;
    }
    private static int delete(CommandSourceStack src,String id) { if(editable(src,id)==null) return 0; REGIONS.delete(id); save(); message(src,"Deleted "+id); return 1; }
    private static int info(CommandSourceStack src,String id) {
        Region r=REGIONS.get(id); if(r==null) {message(src,"Not found");return 0;}
        message(src,r.id+" "+r.dimension+" ("+r.minX+","+r.minY+","+r.minZ+") - ("+r.maxX+","+r.maxY+","+r.maxZ+") priority="+r.priority+" flags="+r.flags); return 1;
    }
    private static int flag(CommandSourceStack src,String id,String name,String value) {
        Region r=editable(src,id); if(r==null) return 0;
        try {
            Flag f=Flag.parse(name);
            if(!f.isText() && !Set.of("allow","deny","none").contains(value.toLowerCase(Locale.ROOT))) {message(src,"Use allow, deny or none");return 0;}
            if(value.equalsIgnoreCase("none")) r.flags.remove(f.name()); else r.flags.put(f.name(),value);
            save(); message(src,"Flag "+name+" = "+value);return 1;
        } catch(IllegalArgumentException e) {message(src,"Unknown flag. Available: "+Flag.names());return 0;}
    }
    private static int priority(CommandSourceStack src,String id,int value) {
        Region r=editable(src,id); if(r==null) return 0; r.priority=value;save();message(src,"Priority = "+value);return 1;
    }
    private static int member(CommandSourceStack src,String id,String uuid,boolean owner,boolean remove) {
        Region r=editable(src,id);if(r==null)return 0;
        try {
            UUID user=UUID.fromString(uuid);Set<UUID> set=owner?r.owners:r.members;
            if(remove && owner && r.owners.size()==1 && r.owners.contains(user)) {message(src,"Cannot remove the last owner");return 0;}
            if(remove)set.remove(user);else set.add(user);
            save();message(src,"Updated "+id);return 1;
        } catch(IllegalArgumentException e) {message(src,"Provide a valid UUID");return 0;}
    }
    private static void save() {try{STORE.save(REGIONS);}catch(Exception e){throw new IllegalStateException("Failed to save region data",e);}}
}
