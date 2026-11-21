package ro.abyssmc.abyssguard;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.item.BucketItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import java.nio.file.Path;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import java.util.*;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

/** Alpha integration. See README for limitations and unverified 26.3 API bindings. */
public final class AbyssGuard implements ModInitializer {
    public static final RegionManager REGIONS=new RegionManager();
    public static final Selection SELECTIONS=new Selection();
    private static final GuardConfig CONFIG=new GuardConfig(Path.of("config","abyssguard"));
    private static final RegionStore STORE=new RegionStore(Path.of("config","abyssguard","regions.json"));
    @Override public void onInitialize() {
        CONFIG.load();
        try { STORE.load(REGIONS); } catch(Exception e) { throw new IllegalStateException("AbyssGuard cannot load region data; refusing to start",e); }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var rg = literal("rg");
            rg.then(literal("menu").requires(src -> access(src,"abyssguard.menu"))
                .executes(ctx -> menu(ctx.getSource(), null))
                .then(argument("id",StringArgumentType.word())
                    .executes(ctx -> menu(ctx.getSource(),StringArgumentType.getString(ctx,"id")))));
            rg.then(literal("reload").requires(src -> access(src,"abyssguard.admin"))
                .executes(ctx -> {CONFIG.load(); message(ctx.getSource(),"reload");return 1;}));
            rg.then(literal("pos1").requires(src -> access(src,"abyssguard.region.create")).executes(ctx -> setPos(ctx.getSource(), 1)));
            rg.then(literal("pos2").requires(src -> access(src,"abyssguard.region.create")).executes(ctx -> setPos(ctx.getSource(), 2)));
            rg.then(literal("define").requires(src -> access(src,"abyssguard.region.create")).then(argument("id", StringArgumentType.word())
                .executes(ctx -> define(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            rg.then(literal("delete").requires(src -> access(src,"abyssguard.region.delete")).then(argument("id", StringArgumentType.word())
                .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            rg.then(literal("info").requires(src -> access(src,"abyssguard.region.info")).then(argument("id", StringArgumentType.word())
                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            rg.then(literal("list").requires(src -> access(src,"abyssguard.region.info")).executes(ctx -> {
                message(ctx.getSource(), "Regions: " + REGIONS.all().stream().map(r -> r.id).toList());
                return 1;
            }));
            rg.then(literal("flag").requires(src -> access(src,"abyssguard.region.edit")).then(argument("id", StringArgumentType.word())
                .then(argument("flag", StringArgumentType.word())
                .then(argument("value", StringArgumentType.greedyString())
                .executes(ctx -> flag(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "flag"), StringArgumentType.getString(ctx, "value")))))));
            rg.then(literal("priority").requires(src -> access(src,"abyssguard.region.edit")).then(argument("id", StringArgumentType.word())
                .then(argument("value", IntegerArgumentType.integer())
                .executes(ctx -> priority(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    IntegerArgumentType.getInteger(ctx, "value"))))));
            rg.then(literal("addmember").requires(src -> access(src,"abyssguard.region.edit")).then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), false, false)))));
            rg.then(literal("removemember").requires(src -> access(src,"abyssguard.region.edit")).then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), false, true)))));
            rg.then(literal("addowner").requires(src -> access(src,"abyssguard.region.edit")).then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), true, false)))));
            rg.then(literal("removeowner").requires(src -> access(src,"abyssguard.region.edit")).then(argument("id", StringArgumentType.word())
                .then(argument("uuid", StringArgumentType.word())
                .executes(ctx -> member(ctx.getSource(), StringArgumentType.getString(ctx, "id"),
                    StringArgumentType.getString(ctx, "uuid"), true, true)))));
            dispatcher.register(rg);
        });
        // Region fall-damage protection: evaluated at the landing position, not at the fall start.
        // This is a region rule for every player, including OP and staff.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p)) return true;
            if (source.is(DamageTypes.FALL)) return regionAllows(p, Flag.FALL_DAMAGE);
            if (source.getEntity() instanceof ServerPlayer || source.getDirectEntity() instanceof ServerPlayer) return regionAllows(p, Flag.PVP);
            // This covers direct mob attacks and mob projectiles whose owner is a mob.
            // Damage from players, fall, fire and environmental sources is unaffected.
            if (source.getEntity() instanceof Mob || source.getDirectEntity() instanceof Mob) {
                return regionAllows(p, Flag.MOB_DAMAGE);
            }
            return true;
        });
        // Natural spawning is handled by NaturalSpawnerMixin, not ENTITY_LOAD.
        // This preserves existing mobs, NPCs, pets and staff-spawned entities.
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->permitted(player,pos,Flag.BLOCK_BREAK));
        AttackBlockCallback.EVENT.register((player,world,hand,pos,direction)->permitted(player,pos,Flag.BLOCK_BREAK) ? InteractionResult.PASS : InteractionResult.FAIL);
        UseBlockCallback.EVENT.register((player,world,hand,hit)-> {
            // Buckets are item interactions, not ordinary block placement. Check both
            // the clicked block and the adjacent position (where fluid is placed).
            if (player.getItemInHand(hand).getItem() instanceof BucketItem) {
                BlockPos clicked = hit.getBlockPos();
                BlockPos adjacent = clicked.relative(hit.getDirection());
                boolean empty = player.getItemInHand(hand).is(Items.BUCKET);
                Flag rule = empty ? Flag.BLOCK_BREAK : Flag.BLOCK_PLACE;
                if (!permitted(player, clicked, rule) || !permitted(player, adjacent, rule))
                    return InteractionResult.FAIL;
            }
            return permitted(player,hit.getBlockPos(),Flag.INTERACT) ? InteractionResult.PASS : InteractionResult.FAIL;
        });
    }
    private static boolean regionAllows(ServerPlayer player, Flag flag) {
        return regionAllows(dim(player), player.blockPosition(), flag);
    }
    public static boolean regionAllows(String dimension, BlockPos pos, Flag flag) {
        for (Region region : REGIONS.at(dimension, pos.getX(), pos.getY(), pos.getZ())) {
            String value = region.flags.get(flag.name());
            if (value == null || value.equalsIgnoreCase("none")) continue;
            return !value.equalsIgnoreCase("deny");
        }
        return true;
    }
    private static boolean permitted(net.minecraft.world.entity.player.Player player,BlockPos pos,Flag flag) {
        if (!(player instanceof ServerPlayer p)) return true;
        String permission=flag==Flag.BLOCK_BREAK?"abyssguard.build":flag==Flag.BLOCK_PLACE?"abyssguard.build":"abyssguard.interact";
        boolean bypass=GuardPermissions.has(p,permission)||GuardPermissions.has(p,"abyssguard.admin");
        return REGIONS.allowed(p.level().dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),p.getUUID(),flag,bypass);
    }
    private static void message(CommandSourceStack src,String text) {
        String content=CONFIG.message(text);
        src.sendSuccess(()->HexText.parse(CONFIG.prefix()+" "+content),false);
    }
    private static boolean access(CommandSourceStack src,String node) {
        ServerPlayer p=player(src);
        return p==null || GuardPermissions.has(p,node)||GuardPermissions.has(p,"abyssguard.admin");
    }
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
            CONFIG.applyDefaults(r); REGIONS.add(r); save(); message(src,"Created region "+r.id); return 1;
        } catch(Exception e) { message(src,"Error: "+e.getMessage()); return 0; }
    }
    private static Region editable(CommandSourceStack src,String id) {
        Region r=REGIONS.get(id); ServerPlayer p=player(src);
        if(r==null || (p!=null && !access(src,"abyssguard.region.edit"))) { message(src,"Region not found or you are not an owner"); return null; }
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

    private static int menu(CommandSourceStack src,String id) {
        ServerPlayer p=player(src);
        if(p==null) {message(src,"players-only");return 0;}
        Region r;
        if(id!=null) r=REGIONS.get(id);
        else {
            var list=REGIONS.at(dim(p),p.getX(),p.getY(),p.getZ());
            if(list.isEmpty()) {message(src,"no-region");return 0;}
            r=list.get(0);
        }
        if(r==null) {message(src,"not-found");return 0;}
        if(!access(src,"abyssguard.region.edit")) {message(src,"no-permission");return 0;}
        openMenu(p,r,0);
        return 1;
    }
    private static final Flag[] MENU_FLAGS={Flag.BLOCK_BREAK, Flag.BLOCK_PLACE, Flag.INTERACT, Flag.FALL_DAMAGE, Flag.MOB_SPAWNING, Flag.MOB_DAMAGE, Flag.PVP, Flag.TNT, Flag.CREEPER_EXPLOSION, Flag.OTHER_EXPLOSION};
    private static void openMenu(ServerPlayer p,Region region,int page) {
        int maxPage=(MENU_FLAGS.length-1)/45;
        int current=Math.max(0,Math.min(page,maxPage));
        SimpleContainer inv=new SimpleContainer(54);
        for(int i=0;i<45;i++) {
            int index=current*45+i;
            if(index>=MENU_FLAGS.length) break;
            Flag flag=MENU_FLAGS[index];
            ItemStack stack=new ItemStack(flag.isText()?Items.PAPER:region.flags.getOrDefault(flag.name(),"allow").equalsIgnoreCase("deny")?Items.REDSTONE:Items.EMERALD);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,HexText.parse("&b"+flag.name().toLowerCase(Locale.ROOT).replace('_','-')+" &7["+region.flags.getOrDefault(flag.name(),"default")+"]"));
            inv.setItem(i,stack);
        }
        inv.setItem(45,new ItemStack(Items.ARROW));
        inv.setItem(49,new ItemStack(Items.BOOK));
        inv.setItem(53,new ItemStack(Items.ARROW));
        p.openMenu(new SimpleMenuProvider((syncId,playerInv,player)->new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6,syncId,playerInv,inv,6) {
            @Override public void clicked(int slot,int button,ContainerInput type,Player who) {
                if(slot<0) return;
                if(slot<54) {
                    if(slot==45 && current>0) {openMenu(p,region,current-1);return;}
                    if(slot==53 && current<maxPage) {openMenu(p,region,current+1);return;}
                    int index=current*45+slot;
                    if(slot<45 && index<MENU_FLAGS.length) {
                        Flag flag=MENU_FLAGS[index];
                        if(!flag.isText() && access(p.createCommandSourceStack(),"abyssguard.region.edit")) {
                            String old=region.flags.getOrDefault(flag.name(),"allow");
                            region.flags.put(flag.name(),old.equalsIgnoreCase("deny")?"allow":"deny");
                            save();openMenu(p,region,current);
                        }
                    }
                    return;
                }
                super.clicked(slot,button,type,who);
            }
            @Override public boolean stillValid(Player who) {return true;}
            @Override public ItemStack quickMoveStack(Player who,int slot) {return ItemStack.EMPTY;}
        },HexText.parse(CONFIG.guiTitle().replace("{region}",region.id))));
    }
    private static void save() {try{STORE.save(REGIONS);}catch(Exception e){throw new IllegalStateException("Failed to save region data",e);}}
}
