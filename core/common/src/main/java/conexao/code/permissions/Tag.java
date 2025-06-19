package conexao.code.permissions;

import java.util.Set;

/**
 * Representa uma tag de jogador com prefixo/sufixo e permissões.
 */
public class Tag {
    private int id;
    private String name;
    private String color;
    private String prefix;
    private String suffix;
    private Set<String> permissions;

    public Tag(int id, String name, String color, String prefix, String suffix, Set<String> permissions) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.prefix = prefix;
        this.suffix = suffix;
        this.permissions = permissions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
