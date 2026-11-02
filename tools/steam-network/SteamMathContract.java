package com.magneticraft2.common.systems.PRESSURE;

/** Standalone tests of the pure-Java math; NOT a substitute for Forge tests. */
public final class SteamMathContract {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (int pipes = 1; pipes <= 256; pipes++) {
            for (int amount = 0; amount <= 1024; amount++) {
                long sum = 0;
                for (int i = 0; i < pipes; i++)
                    sum += SteamNetworkMath.share(amount, pipes, i);
                require(sum == amount, "Partition created or lost steam");
            }
        }
        require(SteamNetworkMath.pressureKPa(0, 0, 3) == 0.0,
                "Empty component has pressure");
        double atRating = SteamNetworkMath.pressureKPa(6, 6 * 453.15, 1);
        require(Math.abs(atRating - 600.0) < 1e-9,
                "Pressure reference mismatch");
        require(SteamNetworkMath.pressureKPa(7, 7 * 453.15, 1) > 600.0,
                "Actual overpressure not detected");
        require(SteamNetworkMath.leakUnits(0, 700) == 0,
                "Empty network leaks gas");
        require(SteamNetworkMath.leakUnits(1, 700) == 1,
                "Leak consumed more than stored");
        require(SteamNetworkMath.leakUnits(100, 700) > 0,
                "Ruptured network never vents");
        System.out.println("PASS SteamNetworkMath: partition conservation, threshold, bounded leak");
    }
}
