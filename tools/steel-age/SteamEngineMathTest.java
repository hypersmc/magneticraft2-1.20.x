import com.magneticraft2.common.systems.steam.SteamEngineMath;

public final class SteamEngineMathTest {
    private static long assertions;
    private static void check(boolean c,String n){assertions++;if(!c)throw new AssertionError(n);}
    private static void near(float a,float b,String n){check(Math.abs(a-b)<0.001F,n+" "+a+" != "+b);}
    public static void main(String[] args){
        var low=SteamEngineMath.drive(119.9,120,500,12,32,32);
        check(!low.available()&&low.rpm()==0&&low.torque()==0,"below threshold");
        var start=SteamEngineMath.drive(120,120,500,12,32,32);
        check(start.available(),"threshold available");near(start.rpm(),12,"minimum rpm");
        near(start.torque(),7.68F,"minimum pressure torque");
        var rated=SteamEngineMath.drive(500,120,500,12,32,32);
        near(rated.rpm(),32,"rated rpm");near(rated.torque(),32,"rated torque");
        var high=SteamEngineMath.drive(900,120,500,12,32,32);
        near(high.rpm(),32,"rpm clamp");near(high.torque(),32,"torque clamp");
        check(SteamEngineMath.milliSteamPerTick(0,32,false,4,1)==4,"idle");
        check(SteamEngineMath.milliSteamPerTick(16,32,false,4,1)==20,"half load");
        check(SteamEngineMath.milliSteamPerTick(99,32,false,4,1)==36,"demand clamp");
        check(SteamEngineMath.milliSteamPerTick(32,32,true,4,1)==4,"overload idle only");
        System.out.println("PASS: "+assertions+" Steam Engine balance assertions.");
        System.out.println("NOT a Forge build, Steam network, Gear V2, multiblock or rendering test.");
    }
}
