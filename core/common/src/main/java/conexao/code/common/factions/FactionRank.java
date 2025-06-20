package conexao.code.common.factions;

public enum FactionRank {
    REI("\u265B"),
    CAVALEIRO("\u265E"),
    SOLDADO("\u2694"),
    ESCUDEIRO("\u26E8");

    private final String icon;
    FactionRank(String icon) {
        this.icon = icon;
    }
    public String getIcon() {
        return icon;
    }
}
