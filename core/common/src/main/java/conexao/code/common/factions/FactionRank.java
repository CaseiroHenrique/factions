package conexao.code.common.factions;

public enum FactionRank {
    /** Rei da facção (apenas um) */
    REI("\u265B"),
    /** Conselheiros do rei (até dois) */
    CONSELHEIRO("\u265D"),
    /** Chefe de guerra (apenas um) */
    CHEFE_GUERRA("\u2694"),
    /** Membros do exército, sem limite */
    EXERCITO("\u2692"),
    /** Plebeus da facção, sem limite */
    PLEBEU("\u26E8");

    private final String icon;
    FactionRank(String icon) {
        this.icon = icon;
    }
    public String getIcon() {
        return icon;
    }
}
