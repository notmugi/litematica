package fi.dy.masa.litematica.gui;

/** Strict absolute block coordinates, bounded before creating or moving a ghost. */
final class QuickPlaceCoordinates
{
    private QuickPlaceCoordinates() {}

    static int[] parse(String x, String y, String z, int minY, int maxYExclusive)
    {
        int[] pos = { Integer.parseInt(x.trim()), Integer.parseInt(y.trim()), Integer.parseInt(z.trim()) };
        if (pos[0] <= -30000000 || pos[0] >= 30000000 || pos[2] <= -30000000 || pos[2] >= 30000000 ||
            pos[1] < minY || pos[1] >= maxYExclusive)
        {
            throw new IllegalArgumentException("Coordinates outside world bounds");
        }
        return pos;
    }
}
