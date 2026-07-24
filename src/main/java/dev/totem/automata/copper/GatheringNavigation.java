package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Candidate mining positions and stuck-target tracking for gathering navigation. */
public final class GatheringNavigation {
    private static final String BEST="deadrecall_gathering_move_best_distance", STUCK="deadrecall_gathering_move_stuck_ticks", SKIPPED="deadrecall_gathering_skipped_targets";
    private GatheringNavigation() { }
    public static boolean moveOrSkip(CopperGolem golem, ServerLevel level, BlockPos target) {
        List<BlockPos> positions = destinations(level, golem, target); boolean started=false;
        for (BlockPos pos:positions) if (started=golem.getNavigation().moveTo(pos.getX()+.5D,pos.getY(),pos.getZ()+.5D,.75D)) break;
        if (positions.isEmpty() || shouldSkip(golem,target,started)) { skip(golem,level,target); return false; } return true;
    }
    public static List<BlockPos> destinations(ServerLevel level, CopperGolem golem, BlockPos target) {
        List<BlockPos> out=new ArrayList<>(); add(level,golem,out,target.above()); for(Direction d:Direction.Plane.HORIZONTAL)add(level,golem,out,target.above().relative(d)); add(level,golem,out,target.below(2)); add(level,golem,out,target.below()); for(Direction d:Direction.Plane.HORIZONTAL){add(level,golem,out,target.relative(d));add(level,golem,out,target.below().relative(d));add(level,golem,out,target.below(2).relative(d));}
        out.sort((a,b)->{int p=Integer.compare(priority(target,a),priority(target,b));return p!=0?p:Double.compare(golem.distanceToSqr(Vec3.atCenterOf(a)),golem.distanceToSqr(Vec3.atCenterOf(b)));});return out;
    }
    private static int priority(BlockPos target,BlockPos pos){return pos.getY()>target.getY()?0:pos.getY()==target.getY()?1:2;}
    private static void add(ServerLevel l,CopperGolem g,List<BlockPos> ps,BlockPos p){if(!ps.contains(p)&&stand(l,g,p))ps.add(p.immutable());}
    private static boolean stand(ServerLevel l,CopperGolem g,BlockPos p){BlockPos floor=p.below();if(!l.isLoaded(p)||!l.isLoaded(floor))return false;BlockState s=l.getBlockState(floor);if(!s.isFaceSturdy(l,floor,Direction.UP))return false;double w=Math.max(.1D,g.getBbWidth())/2D,h=Math.max(.1D,g.getBbHeight());return l.noCollision(g,new AABB(p.getX()+.5D-w,p.getY(),p.getZ()+.5D-w,p.getX()+.5D+w,p.getY()+h,p.getZ()+.5D+w).deflate(1E-7D));}
    private static boolean shouldSkip(CopperGolem g,BlockPos target,boolean started){CompoundTag tag=CopperGolemData.readEntityTag(g);long distance=Math.max(0,Math.round(g.distanceToSqr(Vec3.atCenterOf(target))*1000D)),best=tag.getLongOr(BEST,Long.MAX_VALUE);int stuck=tag.getIntOr(STUCK,0);if(best==Long.MAX_VALUE||distance+250<best){best=distance;stuck=0;}else if(!started||g.getNavigation().isDone()||g.getDeltaMovement().lengthSqr()<1E-4D)stuck++;else if(stuck>0)stuck--;tag.putLong(BEST,best);tag.putInt(STUCK,stuck);CopperGolemData.writeEntityTag(g,tag);return stuck>=80;}
    private static void skip(CopperGolem g,ServerLevel l,BlockPos target){l.destroyBlockProgress(g.getId(),target,-1);CompoundTag tag=CopperGolemData.readEntityTag(g);var values=new ArrayList<>(tag.getList(SKIPPED).map(list->list.stream().map(v->v.asString().orElse("")).filter(v->!v.isBlank()).toList()).orElse(List.of()));String key=Long.toString(target.asLong());if(!values.contains(key)){if(values.size()>=128)values.removeFirst();values.add(key);}net.minecraft.nbt.ListTag list=new net.minecraft.nbt.ListTag();values.forEach(v->list.add(net.minecraft.nbt.StringTag.valueOf(v)));tag.put(SKIPPED,list);GatheringRuntimeState.clearTarget(tag);GatheringRuntimeState.setActivity(tag,CopperGolemActivity.SEARCHING);CopperGolemData.writeEntityTag(g,tag);g.getNavigation().stop();}
}
