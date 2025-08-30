# MC Data Command Plus
for minecraft 1.20+

This mod adds more functionality to /<b>data</b> command.
Lets you:
<ul>
<li>Concatenate strings</li>
<li>Access player data storage (pds)</li>
<li>Loot inventories</li>
<li>Perform math operations in place</li>
</ul>
Go to <a href="https://modrinth.com/mod/data-command-plus">modrinth.com/mod/data-command-plus</a> for Downloads and Wiki
<br>

Syntax of the command fits right into Vanilla syntax. see https://minecraft.wiki/w/Commands/data on Minecraft Wiki

/data modify (block / entity / storage / <b>storageplayer</b>) path (set / insert / ...) <b><u>concat</u></b> (block / entity / storage) path [separator]

<h3>Concatenate</h3>
<ul>
<li>String + String</li>
<li>List of Strings</li>
<li>Any Array</li>
<li>Multiple Nbt Values (selected by nbt selector)</li>
</ul>

<h3>Storageplayer</h3>
stores each player data storage in "Word/playerdata/uuid_pds.dat"

/loot ... inventory ... [MaxSlots] <br>
drops the loot from source inventory into target inventory, limited with MaxSlots

<h3>Math Operation</h3>
performs math operation on the target value, target can be of <b>any numeric type</b>, Error will be thrown and success set to 0 when type is not numeric

center><h2>Delete Kill Command</h2></center>
In response to <a href="https://www.youtube.com/watch?v=TbrO5ONPp3s">So I Added WAY Too Many Slimes to Minecraft -by Fundy</a><br>
/<b>deletekill</b> &lt;entities&gt;<br>
Deletes selected entities, no loot dropped, no events fired just gone<br>
p.s. works grat on slimes
