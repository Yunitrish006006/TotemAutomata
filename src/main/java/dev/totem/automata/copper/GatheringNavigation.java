package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Candidate mining positions and stuck-target tracking for gathering navigation. */
public final class GatheringNavigation {
    private static final String SKIPPED="deadrecall_gathering_skipped_targets";
    private static final int RECOMPUTE_STUCK_TICKS = 10;
    private static final int SKIP_STUCK_TICKS = 80;
    private static final Map<UUID, Checkpoint> TRANSIENT_STATE = new ConcurrentHashMap<>();
    private GatheringNavigation() { }
    public static void forget(CopperGolem golem) { forget(golem.getUUID()); }
    public static void forget(UUID golemId) { TRANSIENT_STATE.remove(golemId); }
    public static void clearTransientState() { TRANSIENT_STATE.clear(); }

    public static boolean moveOrSkip(CopperGolem golem, ServerLevel level, BlockPos target) {
        long gameTime = level.getGameTime();
        Checkpoint previous = TRANSIENT_STATE.get(golem.getUUID());
        Progress progress = progress(golem, target, previous);
        boolean recompute = GatheringNavigationCadence.shouldRecompute(
                previous == null || previous.kind() != Kind.TARGET ? null : previous.target(),
                previous == null ? Long.MIN_VALUE : previous.lastRequestTick(),
                target,
                gameTime,
                golem.getNavigation().isDone(),
                progress.stuckTicks() >= RECOMPUTE_STUCK_TICKS
        );

        BlockPos destination = previous == null || previous.kind() != Kind.TARGET
                ? null : previous.destination();
        long lastRequestTick = previous == null ? Long.MIN_VALUE : previous.lastRequestTick();
        if (recompute) {
            Optional<BlockPos> selected = destinations(level, golem, target).stream().findFirst();
            if (selected.isEmpty()) {
                skip(golem, level, target);
                return false;
            }
            destination = selected.orElseThrow();
            golem.getNavigation().moveTo(
                    destination.getX() + .5D,
                    destination.getY(),
                    destination.getZ() + .5D,
                    .75D
            );
            lastRequestTick = gameTime;
        }

        Checkpoint next = new Checkpoint(
                Kind.TARGET,
                target.immutable(),
                destination == null ? target.immutable() : destination.immutable(),
                lastRequestTick,
                progress.bestDistance(),
                progress.stuckTicks()
        );
        TRANSIENT_STATE.put(golem.getUUID(), next);
        if (progress.stuckTicks() >= SKIP_STUCK_TICKS) {
            skip(golem, level, target);
            return false;
        }
        return true;
    }

    /** Reuses the current path while returning to the configured home. */
    public static void moveHome(CopperGolem golem, ServerLevel level, BlockPos home) {
        long gameTime = level.getGameTime();
        Checkpoint previous = TRANSIENT_STATE.get(golem.getUUID());
        Progress progress = progress(golem, home, previous);
        boolean recompute = GatheringNavigationCadence.shouldRecompute(
                previous == null || previous.kind() != Kind.HOME ? null : previous.target(),
                previous == null ? Long.MIN_VALUE : previous.lastRequestTick(),
                home,
                gameTime,
                golem.getNavigation().isDone(),
                progress.stuckTicks() >= RECOMPUTE_STUCK_TICKS
        );
        long lastRequestTick = previous == null ? Long.MIN_VALUE : previous.lastRequestTick();
        if (recompute) {
            golem.getNavigation().moveTo(home.getX()+.5D, home.getY(), home.getZ()+.5D, .75D);
            lastRequestTick = gameTime;
        }
        TRANSIENT_STATE.put(golem.getUUID(), new Checkpoint(
                Kind.HOME,
                home.immutable(),
                home.immutable(),
                lastRequestTick,
                progress.bestDistance(),
                progress.stuckTicks()
        ));
    }

    public static List<BlockPos> destinations(ServerLevel level, CopperGolem golem, BlockPos target) {
        List<BlockPos> out=new ArrayList<>(); add(level,golem,out,target.above()); for(Direction d:Direction.Plane.HORIZONTAL)add(level,golem,out,target.above().relative(d)); add(level,golem,out,target.below(2)); add(level,golem,out,target.below()); for(Direction d:Direction.Plane.HORIZONTAL){add(level,golem,out,target.relative(d));add(level,golem,out,target.below().relative(d));add(level,golem,out,target.below(2).relative(d));}
        out.sort((a,b)->{int p=Integer.compare(priority(target,a),priority(target,b));return p!=0?p:Double.compare(golem.distanceToSqr(Vec3.atCenterOf(a)),golem.distanceToSqr(Vec3.atCenterOf(b)));});return out;
    }
    private static int priority(BlockPos target,BlockPos pos){return pos.getY()>target.getY()?0:pos.getY()==target.getY()?1:2;}
    private static void add(ServerLevel l,CopperGolem g,List<BlockPos> ps,BlockPos p){if(!ps.contains(p)&&stand(l,g,p))ps.add(p.immutable());}
    private static boolean stand(ServerLevel l,CopperGolem g,BlockPos p){BlockPos floor=p.below();if(!l.isLoaded(p)||!l.isLoaded(floor))return false;BlockState s=l.getBlockState(floor);if(!s.isFaceSturdy(l,floor,Direction.UP))return false;double w=Math.max(.1D,g.getBbWidth())/2D,h=Math.max(.1D,g.getBbHeight());return l.noCollision(g,new AABB(p.getX()+.5D-w,p.getY(),p.getZ()+.5D-w,p.getX()+.5D+w,p.getY()+h,p.getZ()+.5D+w).deflate(1E-7D));}
    private static Progress progress(CopperGolem golem, BlockPos target, Checkpoint checkpoint) {
        long distance = Math.max(0, Math.round(golem.distanceToSqr(Vec3.atCenterOf(target)) * 1000D));
        if (checkpoint == null || !checkpoint.target().equals(target)) {
            return new Progress(distance, 0);
        }
        long best = checkpoint.bestDistance();
        int stuck = checkpoint.stuckTicks();
        if (distance + 250 < best) {
            best = distance;
            stuck = 0;
        } else if (golem.getNavigation().isDone() || golem.getDeltaMovement().lengthSqr() < 1E-4D) {
            stuck++;
        } else if (stuck > 0) {
            stuck--;
        }
        return new Progress(best, stuck);
    }

    private static void skip(CopperGolem g,ServerLevel l,BlockPos target){l.destroyBlockProgress(g.getId(),target,-1);var tag=CopperGolemData.readEntityTag(g);var values=new ArrayList<>(tag.getList(SKIPPED).map(list->list.stream().map(v->v.asString().orElse("")).filter(v->!v.isBlank()).toList()).orElse(List.of()));String key=Long.toString(target.asLong());if(!values.contains(key)){if(values.size()>=128)values.removeFirst();values.add(key);}net.minecraft.nbt.ListTag list=new net.minecraft.nbt.ListTag();values.forEach(v->list.add(net.minecraft.nbt.StringTag.valueOf(v)));tag.put(SKIPPED,list);GatheringRuntimeState.clearTarget(tag);GatheringRuntimeState.setActivity(tag,CopperGolemActivity.SEARCHING);CopperGolemData.writeEntityTag(g,tag);g.getNavigation().stop();TRANSIENT_STATE.remove(g.getUUID());}

    private enum Kind { TARGET, HOME }
    private record Progress(long bestDistance, int stuckTicks) { }
    private record Checkpoint(
            Kind kind,
            BlockPos target,
            BlockPos destination,
            long lastRequestTick,
            long bestDistance,
            int stuckTicks
    ) { }
}
