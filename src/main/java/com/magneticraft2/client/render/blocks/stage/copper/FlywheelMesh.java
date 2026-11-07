package com.magneticraft2.client.render.blocks.stage.copper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Shared six-face flywheel box. Legacy Wood UVs unchanged; Iron uses
 * proportional, native-density UVs (16 texels per 1 block). */
public final class FlywheelMesh {
    private FlywheelMesh(){}
    public static void drawBox(PoseStack stack, VertexConsumer out,int light,
            double sx,double sy,double sz,boolean nativeUv){
        double x=-sx/2d,X=sx/2d,y=-sy/2d,Y=sy/2d,z=-sz/2d,Z=sz/2d;
        float u=nativeUv?(float)sx:1f,v=nativeUv?(float)sy:1f,w=nativeUv?(float)sz:1f;
        var pose=stack.last();
        quad(out,pose,light,x,Y,z,x,Y,Z,X,Y,Z,X,Y,z,0,1,0,u,w);
        quad(out,pose,light,x,y,Z,x,y,z,X,y,z,X,y,Z,0,-1,0,u,w);
        quad(out,pose,light,x,y,Z,X,y,Z,X,Y,Z,x,Y,Z,0,0,1,u,v);
        quad(out,pose,light,X,y,z,x,y,z,x,Y,z,X,Y,z,0,0,-1,u,v);
        quad(out,pose,light,X,y,Z,X,y,z,X,Y,z,X,Y,Z,1,0,0,w,v);
        quad(out,pose,light,x,y,z,x,y,Z,x,Y,Z,x,Y,z,-1,0,0,w,v);
    }
    private static void quad(VertexConsumer out,PoseStack.Pose pose,int light,
            double x0,double y0,double z0,double x1,double y1,double z1,
            double x2,double y2,double z2,double x3,double y3,double z3,
            float nx,float ny,float nz,float u,float v){
        vertex(out,pose,light,x0,y0,z0,0,0,nx,ny,nz);
        vertex(out,pose,light,x1,y1,z1,0,v,nx,ny,nz);
        vertex(out,pose,light,x2,y2,z2,u,v,nx,ny,nz);
        vertex(out,pose,light,x3,y3,z3,u,0,nx,ny,nz);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,int light,
            double x,double y,double z,float u,float v,float nx,float ny,float nz){
        out.vertex(pose.pose(),(float)x,(float)y,(float)z)
                .color(1f,1f,1f,1f).uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(),nx,ny,nz).endVertex();
    }
}
