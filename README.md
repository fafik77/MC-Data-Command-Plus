# Mc_Data_Concat-1.20

This mod adds more functionality to /data command.
<ul>
<li>lets you concatenate strings</li>
<li>lets you access player data storage (pds)</li>
</ul>
Get it on https://legacy.curseforge.com/minecraft/mc-mods/data-concat-string
</br>

Syntax of the command fits right into Vanilla syntax. see https://minecraft.wiki/w/Commands/data on Minecraft Wiki

/data modify (block / entity / storage / <b>storageplayer</b>) path (set / insert / ...) <b><u>concat</u></b> (block / entity / storage) path [separator]

Concatenate:
<ul>
<li>String + String</li>
<li>List of Strings</li>
<li>Any Array</li>
<li>Multiple Nbt Values (selected by nbt selector)</li>
</ul>

storageplayer:<br>
stores each player data storage in "Word/playerdata/uuid_pds.dat"
