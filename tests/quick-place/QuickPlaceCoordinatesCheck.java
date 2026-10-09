package fi.dy.masa.litematica.gui;

public final class QuickPlaceCoordinatesCheck
{
    public static void main(String[] args)
    {
        int[] p = QuickPlaceCoordinates.parse(" -29999999 ", "-64", "+29999999", -64, 320);
        assert p[0] == -29999999 && p[1] == -64 && p[2] == 29999999;
        assert QuickPlaceCoordinates.parse("0", "319", "0", -64, 320)[1] == 319;
        assert QuickPlaceCoordinates.parse("0", "2047", "0", -2048, 2048)[1] == 2047;
        for (String[] invalid : new String[][] {
                {"30000000", "0", "0"}, {"0", "0", "-30000000"},
                {"0", "320", "0"}, {"0", "-65", "0"}, {"0", "1.5", "0"},
                {"2147483648", "0", "0"}, {"0", "", "0"}, {"~1", "0", "0"},
                {"NaN", "0", "0"}, {"0", "1 2", "0"}})
        {
            try
            {
                QuickPlaceCoordinates.parse(invalid[0], invalid[1], invalid[2], -64, 320);
                throw new AssertionError(java.util.Arrays.toString(invalid));
            }
            catch (IllegalArgumentException expected) { }
        }
        System.out.println("Quick-place coordinate checks passed");
    }
}
