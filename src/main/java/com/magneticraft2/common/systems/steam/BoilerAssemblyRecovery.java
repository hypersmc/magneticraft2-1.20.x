package com.magneticraft2.common.systems.steam;

import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Boiler-only deferred teardown. Live structures keep their existing controller
 * ledger. This SavedData contains only pending recovery and small attachment
 * identity stamps, not a second simulation or a copy of the live drum ledger.
 * All world writes occur on the logical server. No chunk is force-loaded.
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID)
public final class BoilerAssemblyRecovery extends SavedData {
    private static final String NAME = "magneticraft2_boiler_recovery_v1";
    private static final Set<ServerLevel> QUEUED = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    public record Attachment(BlockPos position, BlockPos ownerPosition, UUID owner,
                             UUID token, BlockState state) { }
    private static final class Request {
        final BoilerAssemblyHost.Link owner;
        boolean drops;
        final Set<Long> lost = new HashSet<>();
        Request(BoilerAssemblyHost.Link owner, boolean drops) { this.owner = owner; this.drops = drops; }
    }
    private record Entry(BlockPos position, BlockState original, @Nullable UUID attachmentToken, boolean lost) { }
    private record Job(UUID identity, BlockPos ownerPosition, boolean drops,
                       BoilerRecoveryLedger<Entry> entries) { }

    private final Map<UUID, BoilerAssemblyHost.Link> unlinks = new LinkedHashMap<>();
    private final Map<UUID, Request> requests = new LinkedHashMap<>();
    private final Map<UUID, Job> jobs = new LinkedHashMap<>();
    private final Map<Long, Attachment> attachments = new LinkedHashMap<>();
    private final Set<Long> removing = new HashSet<>();
    private boolean processing;

    public BoilerAssemblyRecovery() { }
    private static BoilerAssemblyRecovery get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BoilerAssemblyRecovery::load, BoilerAssemblyRecovery::new, NAME);
    }
    private static @Nullable BoilerAssemblyRecovery existing(ServerLevel level) {
        return level.getDataStorage().get(BoilerAssemblyRecovery::load, NAME);
    }
    public static void wake(ServerLevel level) { QUEUED.add(level); }

    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) {
        // ChunkEvent may precede FULL status. Do not touch world blocks here.
        if (event.getLevel() instanceof ServerLevel server)
            server.getServer().execute(() -> wake(server));
    }
    @SubscribeEvent public static void endTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel server)
                || !QUEUED.remove(server)) return;
        BoilerAssemblyRecovery data = existing(server);
        if (data != null) data.process(server);
    }

    public static void unlinkParent(ServerLevel level, BoilerAssemblyHost.Link parent, UUID child) {
        BoilerAssemblyRecovery data = get(level);
        data.unlinks.put(child, parent);
        data.setDirty();
        data.process(level);
    }

    public static boolean attachmentAreaLoaded(ServerLevel level, BlockPos vessel, net.minecraft.core.Direction facing) {
        for (BoilerServiceFittingBlock.Role role : retainedRoles())
            if (!level.hasChunkAt(BoilerServiceFittingBlock.attachmentPosition(vessel, role, facing))) return false;
        return true;
    }
    private static BoilerServiceFittingBlock.Role[] retainedRoles() {
        return new BoilerServiceFittingBlock.Role[]{BoilerServiceFittingBlock.Role.WATER,
                BoilerServiceFittingBlock.Role.STEAM, BoilerServiceFittingBlock.Role.SAFETY};
    }
    public static void markHost(ServerLevel level, BoilerAssemblyHost owner) {
        for (Map.Entry<BlockPos, BlockState> entry : owner.boilerOriginalBlocks().entrySet()) {
            if (entry.getValue().isAir() || !level.hasChunkAt(entry.getKey())) continue;
            if (level.getBlockEntity(entry.getKey()) instanceof Multiblockfiller_tile filler)
                filler.setBoilerAssemblyIdentity(owner.boilerAssemblyId());
        }
    }
    public static void adoptExistingServiceFittings(ServerLevel level, SteelBoilerVesselBlockEntity vessel,
                                                   net.minecraft.core.Direction facing) {
        for (BoilerServiceFittingBlock.Role role : retainedRoles()) {
            BlockPos pos = BoilerServiceFittingBlock.attachmentPosition(vessel.getBlockPos(), role, facing);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BoilerServiceFittingBlock fitting && fitting.role() == role
                    && state.getValue(BoilerServiceFittingBlock.FACING) == facing)
                trackAttachment(level, vessel, pos, state);
        }
    }
    public static void trackAttachment(ServerLevel level, BoilerAssemblyHost owner, BlockPos pos, BlockState state) {
        if (!owner.boilerAssemblyTracked() || !owner.boilerAssemblyFormed()) return;
        BoilerAssemblyRecovery data = get(level);
        Attachment old = data.attachments.get(pos.asLong());
        if (old != null && old.owner.equals(owner.boilerAssemblyId()) && old.state.equals(state)) return;
        data.attachments.put(pos.asLong(), new Attachment(pos.immutable(), owner.boilerAssemblyPosition(),
                owner.boilerAssemblyId(), UUID.randomUUID(), state));
        data.setDirty();
    }
    public static @Nullable Attachment attachment(ServerLevel level, BlockPos pos) {
        BoilerAssemblyRecovery data = existing(level);
        return data == null ? null : data.attachments.get(pos.asLong());
    }

    public static void request(ServerLevel level, BoilerAssemblyHost.Link owner, boolean drops, @Nullable BlockPos lost) {
        BoilerAssemblyRecovery data = get(level);
        data.requestInternal(owner, drops, lost);
        data.process(level);
    }
    private void requestInternal(BoilerAssemblyHost.Link owner, boolean drops, @Nullable BlockPos lost) {
        Job running = jobs.get(owner.identity());
        if (running != null) {
            if (lost != null) running.entries.updateIfPresent(lost.asLong(),
                    entry -> new Entry(entry.position, entry.original, entry.attachmentToken, true));
            if (!drops && running.drops) jobs.put(owner.identity(),
                    new Job(running.identity, running.ownerPosition, false, running.entries));
            setDirty();
            return;
        }
        Request request = requests.computeIfAbsent(owner.identity(), key -> new Request(owner, drops));
        request.drops &= drops; // a creative/no-drop request cannot become a survival duplication path
        if (lost != null) request.lost.add(lost.asLong());
        setDirty();
    }
    private static @Nullable BoilerAssemblyHost loadedHost(ServerLevel level, BoilerAssemblyHost.Link link) {
        if (!level.hasChunkAt(link.position())) return null;
        BlockEntity entity = level.getBlockEntity(link.position());
        return entity instanceof BoilerAssemblyHost host && host.boilerAssemblyTracked()
                && host.boilerAssemblyId().equals(link.identity()) ? host : null;
    }
    public static @Nullable BoilerAssemblyHost.Link ownerOf(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BoilerAssemblyHost host && host.boilerAssemblyTracked() && host.boilerAssemblyFormed())
            return new BoilerAssemblyHost.Link(pos.immutable(), host.boilerAssemblyId());
        if (entity instanceof Multiblockfiller_tile filler && filler.getBoilerAssemblyIdentity() != null)
            return new BoilerAssemblyHost.Link(filler.getControllerPos(), filler.getBoilerAssemblyIdentity());
        return null;
    }
    public static boolean handles(Level level, BlockPos pos) { return ownerOf(level, pos) != null; }

    /** False means no mutation. Callers still suppress vanilla removal for handled assemblies. */
    public static boolean playerBreak(Level world, BlockPos clicked, Player player) {
        if (!(world instanceof ServerLevel level)) return false;
        BoilerAssemblyHost.Link link = ownerOf(level, clicked);
        if (link == null) return false;
        BoilerAssemblyHost host = loadedHost(level, link);
        if (host == null) return refuse(player, "unloaded");
        if (!player.getAbilities().instabuild && !player.getMainHandItem().is(ItemTags.PICKAXES))
            return refuse(player, "pickaxe");
        BoilerAssemblyRecovery data = get(level);
        List<BoilerAssemblyHost> owners = new ArrayList<>(); owners.add(host);
        BoilerAssemblyHost.Link child = host.boilerChild();
        if (child != null) {
            BoilerAssemblyHost childHost = loadedHost(level, child);
            if (childHost == null) return refuse(player, "unloaded");
            owners.add(childHost);
        }
        Set<BlockPos> affected = new HashSet<>();
        for (BoilerAssemblyHost owner : owners) {
            affected.addAll(owner.boilerOriginalBlocks().keySet());
            for (Attachment part : data.attachments.values())
                if (part.owner.equals(owner.boilerAssemblyId())) affected.add(part.position);
        }
        if (!BoilerFireboxLayout.loaded(level, affected)) return refuse(player, "unloaded");
        for (BlockPos pos : affected) {
            if (!player.getAbilities().mayBuild || !level.mayInteract(player, pos)) return refuse(player, "protected");
            BlockState state = level.getBlockState(pos);
            // The clicked block's BreakEvent has already run. Check the other actual targets too.
            if (!pos.equals(clicked) && !state.isAir() && MinecraftForge.EVENT_BUS.post(
                    new BlockEvent.BreakEvent(level, pos, state, player))) return refuse(player, "protected");
        }
        data.requestInternal(link, !player.getAbilities().instabuild, null);
        data.process(level);
        return true;
    }
    private static boolean refuse(Player player, String reason) {
        player.displayClientMessage(Component.translatable("message.magneticraft2.firebox_v2." + reason), true);
        return false;
    }

    /** Called before a tracked controller/filler BE disappears, including explosions. */
    public static void unexpectedRemoval(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel level)) return;
        BoilerAssemblyRecovery data = existing(level);
        if (data != null && data.removing.contains(pos.asLong())) return;
        BoilerAssemblyHost.Link owner = ownerOf(level, pos);
        if (owner != null) request(level, owner, true, pos);
    }

    /** Vanilla owns the individually broken fitting's own loot, not this method. */
    public static boolean attachmentRemoved(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel level)) return false;
        BoilerAssemblyRecovery data = existing(level);
        if (data == null) return false;
        if (data.removing.contains(pos.asLong())) return true;
        Attachment old = data.attachments.remove(pos.asLong());
        if (old == null) return false;
        data.setDirty();
        if (old.state.getBlock() instanceof BoilerServiceFittingBlock fitting
                && fitting.role() == BoilerServiceFittingBlock.Role.CHIMNEY)
            data.queueUpperStack(old, pos.getY() + 1, true);
        data.process(level);
        return true;
    }
    public static boolean playerBreakStack(Level world, BlockPos pos, Player player) {
        if (!(world instanceof ServerLevel level)) return false;
        BoilerAssemblyRecovery data = existing(level);
        Attachment at = data == null ? null : data.attachments.get(pos.asLong());
        if (at == null) return false;
        List<Attachment> parts = data.upperStack(at, pos.getY());
        for (Attachment part : parts) {
            if (!level.hasChunkAt(part.position)) return refuse(player, "unloaded");
            if (!level.mayInteract(player, part.position) || !player.getAbilities().mayBuild)
                return refuse(player, "protected");
            if (!part.position.equals(pos) && MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(
                    level, part.position, level.getBlockState(part.position), player))) return refuse(player, "protected");
        }
        if (!player.getAbilities().instabuild && !player.getMainHandItem().is(ItemTags.PICKAXES))
            return refuse(player, "pickaxe");
        data.queueUpperStack(at, pos.getY(), !player.getAbilities().instabuild);
        data.process(level);
        return true;
    }
    private List<Attachment> upperStack(Attachment at, int minimumY) {
        return attachments.values().stream().filter(a -> a.owner.equals(at.owner)
                && a.position.getX() == at.position.getX() && a.position.getZ() == at.position.getZ()
                && a.position.getY() >= minimumY && a.position.getY() <= at.position.getY() + 2
                && a.state.getBlock() instanceof BoilerServiceFittingBlock fitting
                && fitting.role() == BoilerServiceFittingBlock.Role.CHIMNEY).toList();
    }
    private void queueUpperStack(Attachment at, int minimumY, boolean drops) {
        Map<Long, Entry> entries = new LinkedHashMap<>();
        for (Attachment part : upperStack(at, minimumY)) entries.put(part.position.asLong(),
                new Entry(part.position, part.state, part.token, false));
        if (entries.isEmpty()) return;
        UUID jobKey = UUID.randomUUID();
        jobs.put(jobKey, new Job(at.owner, at.ownerPosition, drops, new BoilerRecoveryLedger<>(entries)));
        setDirty();
    }

    private void process(ServerLevel level) {
        if (processing) return;
        processing = true;
        try {
            // At most two structural owners are linked. No tick-time topology scanning.
            for (int round = 0; round < 2; round++) {
                for (Request request : new ArrayList<>(requests.values())) {
                    BoilerAssemblyHost host = loadedHost(level, request.owner);
                    if (host == null) continue;
                    BoilerAssemblyHost.Link child = host.boilerChild();
                    Map<BlockPos, BlockState> originals = host.claimBoilerOriginalBlocks();
                    Map<Long, Entry> entries = new LinkedHashMap<>();
                    for (Map.Entry<BlockPos, BlockState> entry : originals.entrySet())
                        if (!entry.getValue().isAir()) entries.put(entry.getKey().asLong(), new Entry(
                                entry.getKey().immutable(), entry.getValue(), null, request.lost.contains(entry.getKey().asLong())));
                    for (Attachment attachment : attachments.values())
                        if (attachment.owner.equals(request.owner.identity())) entries.put(attachment.position.asLong(),
                                new Entry(attachment.position, attachment.state, attachment.token, false));
                    requests.remove(request.owner.identity());
                    if (!entries.isEmpty()) jobs.put(request.owner.identity(), new Job(request.owner.identity(),
                            request.owner.position(), request.drops, new BoilerRecoveryLedger<>(entries)));
                    if (child != null) requestInternal(child, request.drops, null);
                    setDirty();
                }
            }
            for (Map.Entry<UUID, BoilerAssemblyHost.Link> unlink : new ArrayList<>(unlinks.entrySet())) {
                BoilerAssemblyHost.Link parent = unlink.getValue();
                if (!level.hasChunkAt(parent.position())) continue;
                BlockEntity entity = level.getBlockEntity(parent.position());
                if (entity instanceof SteelBoilerVesselBlockEntity vessel
                        && vessel.boilerAssemblyId().equals(parent.identity()))
                    vessel.unlinkBoilerFirebox(unlink.getKey());
                // A loaded different/missing owner is not ours to change.
                unlinks.remove(unlink.getKey());
                setDirty();
            }
            for (Map.Entry<UUID, Job> jobPair : new ArrayList<>(jobs.entrySet())) {
                Job job = jobPair.getValue();
                for (Entry entry : job.entries.snapshot().values()) {
                    if (!level.hasChunkAt(entry.position)) continue;
                    boolean matches = matches(level, job, entry);
                    // Consume before world callbacks; repeated callbacks cannot refund twice.
                    job.entries.claim(entry.position.asLong());
                    setDirty();
                    if (!matches && !entry.lost) continue; // never touch unrelated replacement blocks
                    if (entry.attachmentToken != null) {
                        Attachment stamp = attachments.get(entry.position.asLong());
                        if (stamp != null && stamp.token.equals(entry.attachmentToken)) attachments.remove(entry.position.asLong());
                    }
                    removing.add(entry.position.asLong());
                    try {
                        if (matches && !entry.lost) level.setBlock(entry.position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        if (job.drops) Block.popResource(level, entry.position, new ItemStack(entry.original.getBlock()));
                    } finally { removing.remove(entry.position.asLong()); }
                }
                if (job.entries.isEmpty()) { jobs.remove(jobPair.getKey()); setDirty(); }
            }
        } finally { processing = false; }
    }
    private boolean matches(ServerLevel level, Job job, Entry entry) {
        if (entry.attachmentToken != null) {
            Attachment stamp = attachments.get(entry.position.asLong());
            return stamp != null && stamp.owner.equals(job.identity) && stamp.token.equals(entry.attachmentToken)
                    && level.getBlockState(entry.position).equals(entry.original);
        }
        BlockEntity entity = level.getBlockEntity(entry.position);
        if (entry.position.equals(job.ownerPosition))
            return entity instanceof BoilerAssemblyHost host && host.boilerAssemblyId().equals(job.identity);
        return entity instanceof Multiblockfiller_tile filler
                && job.identity.equals(filler.getBoilerAssemblyIdentity())
                && job.ownerPosition.equals(filler.getControllerPos());
    }

    private static CompoundTag writeState(BlockState state) {
        return (CompoundTag) BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().orElseThrow();
    }
    private static @Nullable BlockState readState(CompoundTag tag) {
        return BlockState.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", 1);
        ListTag waiting = new ListTag();
        for (Request request : requests.values()) {
            CompoundTag t = new CompoundTag(); t.putUUID("Id", request.owner.identity());
            t.putLong("Owner", request.owner.position().asLong()); t.putBoolean("Drops", request.drops);
            t.putLongArray("Lost", request.lost.stream().mapToLong(Long::longValue).toArray()); waiting.add(t);
        }
        tag.put("Requests", waiting);
        ListTag orphanLinks = new ListTag();
        for (Map.Entry<UUID, BoilerAssemblyHost.Link> entry : unlinks.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Child", entry.getKey()); t.putUUID("ParentId", entry.getValue().identity());
            t.putLong("Parent", entry.getValue().position().asLong()); orphanLinks.add(t);
        }
        tag.put("Unlinks", orphanLinks);
        ListTag work = new ListTag();
        for (Map.Entry<UUID, Job> jobPair : jobs.entrySet()) {
            Job job = jobPair.getValue(); CompoundTag t = new CompoundTag(); t.putUUID("Job", jobPair.getKey());
            t.putUUID("Id", job.identity); t.putLong("Owner", job.ownerPosition.asLong()); t.putBoolean("Drops", job.drops);
            ListTag entries = new ListTag();
            for (Entry entry : job.entries.snapshot().values()) {
                CompoundTag e = new CompoundTag(); e.putLong("Pos", entry.position.asLong());
                e.put("Original", writeState(entry.original)); e.putBoolean("Lost", entry.lost);
                if (entry.attachmentToken != null) e.putUUID("Attachment", entry.attachmentToken);
                entries.add(e);
            }
            t.put("Entries", entries); work.add(t);
        }
        tag.put("Jobs", work);
        ListTag installed = new ListTag();
        for (Attachment a : attachments.values()) {
            CompoundTag t = new CompoundTag(); t.putLong("Pos", a.position.asLong());
            t.putLong("Owner", a.ownerPosition.asLong()); t.putUUID("Id", a.owner); t.putUUID("Token", a.token);
            t.put("State", writeState(a.state)); installed.add(t);
        }
        tag.put("Attachments", installed);
        return tag;
    }
    private static BoilerAssemblyRecovery load(CompoundTag tag) {
        BoilerAssemblyRecovery data = new BoilerAssemblyRecovery();
        if (tag.getInt("Version") != 1)
            throw new IllegalStateException("Unsupported boiler recovery data version: " + tag.getInt("Version"));
        for (Tag value : tag.getList("Requests", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) value;
            if (!t.hasUUID("Id")) continue;
            Request r = new Request(new BoilerAssemblyHost.Link(BlockPos.of(t.getLong("Owner")), t.getUUID("Id")), t.getBoolean("Drops"));
            for (long lost : t.getLongArray("Lost")) r.lost.add(lost);
            data.requests.put(r.owner.identity(), r);
        }
        for (Tag value : tag.getList("Unlinks", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) value;
            if (t.hasUUID("Child") && t.hasUUID("ParentId"))
                data.unlinks.put(t.getUUID("Child"), new BoilerAssemblyHost.Link(
                        BlockPos.of(t.getLong("Parent")), t.getUUID("ParentId")));
        }
        for (Tag value : tag.getList("Jobs", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) value;
            if (!t.hasUUID("Id") || !t.hasUUID("Job")) continue;
            Map<Long, Entry> entries = new LinkedHashMap<>();
            ListTag source = t.getList("Entries", Tag.TAG_COMPOUND);
            if (source.size() > 512) continue;
            for (Tag raw : source) {
                CompoundTag e = (CompoundTag) raw; BlockState original = readState(e.getCompound("Original"));
                if (original == null || original.isAir()) continue;
                BlockPos pos = BlockPos.of(e.getLong("Pos"));
                entries.put(pos.asLong(), new Entry(pos, original,
                        e.hasUUID("Attachment") ? e.getUUID("Attachment") : null, e.getBoolean("Lost")));
            }
            data.jobs.put(t.getUUID("Job"), new Job(t.getUUID("Id"), BlockPos.of(t.getLong("Owner")),
                    t.getBoolean("Drops"), new BoilerRecoveryLedger<>(entries)));
        }
        for (Tag value : tag.getList("Attachments", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) value;
            if (!t.hasUUID("Id") || !t.hasUUID("Token")) continue;
            BlockState state = readState(t.getCompound("State"));
            if (state == null || !(state.getBlock() instanceof BoilerServiceFittingBlock)) continue;
            BlockPos pos = BlockPos.of(t.getLong("Pos"));
            data.attachments.put(pos.asLong(), new Attachment(pos, BlockPos.of(t.getLong("Owner")),
                    t.getUUID("Id"), t.getUUID("Token"), state));
        }
        return data;
    }
}
