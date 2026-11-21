package ro.abyssmc.abyssguard;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Editable UTF-8 text configurations; missing files generated automatically. */
public final class GuardConfig {
    private final Path dir;
    private final Properties messages=new Properties(), defaults=new Properties();
    public GuardConfig(Path dir){this.dir=dir;}
    public synchronized void load(){
        try {
            Files.createDirectories(dir);
            Path msg=dir.resolve("message.txt"), flags=dir.resolve("protectii.txt");
            if(!Files.exists(msg)) Files.writeString(msg,MESSAGES,StandardCharsets.UTF_8);
            if(!Files.exists(flags)) Files.writeString(flags,PROTECTIONS,StandardCharsets.UTF_8);
            messages.clear();defaults.clear();
            try(var reader=Files.newBufferedReader(msg,StandardCharsets.UTF_8)){messages.load(reader);}
            try(var reader=Files.newBufferedReader(flags,StandardCharsets.UTF_8)){defaults.load(reader);}
        } catch(IOException e){throw new IllegalStateException("Cannot load AbyssGuard configuration",e);}
    }
    public synchronized String prefix(){return messages.getProperty("prefix","&b[AbyssGuard]");}
    public synchronized String guiTitle(){return messages.getProperty("gui-title","&bAbyssGuard &8- {region}");}
    public synchronized String message(String key){return messages.getProperty(key,key);}
    public synchronized void applyDefaults(Region region){
        for(Flag flag:Flag.values()) {
            String value=defaults.getProperty(flag.name().toLowerCase(Locale.ROOT).replace('_','-'));
            if(value==null||flag.isText())continue;
            value=value.strip().toLowerCase(Locale.ROOT);
            if(value.equals("allow")||value.equals("deny"))region.flags.put(flag.name(),value);
        }
    }
    private static final String MESSAGES="""
# AbyssGuard messages. UTF-8. Supports &#RRGGBB, &a, &l, &r.
# Keep the prefix on one line; each letter can have a different HEX color.
prefix=&#00D6FF&lᴀ&#00CDFF&lʙ&#00C4FF&lʏ&#00BBFF&lꜱ&#00B1FF&lꜱ&#00A7FF&lɢ&#009CFF&lᴜ&#0091FF&lᴀ&#0085FF&lʀ&#0079FF&lᴅ &8»
gui-title=&#00D6FF&lAbyssGuard &8| &f{region}
reload=&aConfiguratia a fost reincarcata.
players-only=&cComanda poate fi folosita doar in joc.
no-region=&cNu esti intr-o regiune. Foloseste /rg menu <nume>.
not-found=&cRegiunea nu exista.
no-permission=&cNu ai permisiunea necesara.
""";
    private static final String PROTECTIONS="""
# AbyssGuard defaults for newly defined regions ONLY. Existing regions are unchanged.
# allow = permitted; deny = blocked. Staff with abyssguard.build can bypass build flags.
# Staff permission assignments are controlled exclusively through LuckPerms.
build=deny
block-break=deny
block-place=deny
interact=deny
use=deny
chest-access=deny
pvp=deny
mob-spawning=deny
mob-damage=deny
creeper-explosion=deny
tnt=deny
other-explosion=deny
fire-spread=deny
lava-fire=deny
lava-flow=deny
water-flow=deny
leaf-decay=allow
snow-fall=allow
snow-melt=allow
ice-form=allow
ice-melt=allow
grass-growth=allow
entry=allow
exit=allow
invincibility=deny
fall-damage=allow
enderpearl=deny
teleport=allow
item-drop=allow
item-pickup=allow
vehicle-place=deny
vehicle-destroy=deny
lighter=deny
sleep=allow
feed=allow
damage-animals=deny
""";
}
