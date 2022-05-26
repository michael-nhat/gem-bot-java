package com.gmm.bot.enumeration;

public enum GemModifier {
    NONE(0),
    MANA(1),
    HIT_POINT(2),
    BUFF_ATTACK(3),
    POINT(4),
    EXTRA_TURN(5),
    EXPLODE_HORIZONTAL(6),
    EXPLODE_VERTICAL(7),
    EXPLODE_SQUARE(8);

    public static final int SIZE = values().length;

    private  final int code;

    GemModifier(int code){
        this.code = code;
    }

    public int getCode() {
        return  code;
    }

    public static GemModifier from(int value){
        for (GemModifier modifier : values()){
            if (modifier.code == value) return modifier;
        }
        return null;
    }

    public int getPriorityScore() {
        switch (this) {
            case EXTRA_TURN:
                return 20;
            case EXPLODE_SQUARE:
            case EXPLODE_VERTICAL:
            case EXPLODE_HORIZONTAL:
                return 10;
            case MANA:
            case HIT_POINT:
            case BUFF_ATTACK:
                return 3;
            default:
                return 0;
        }
    }
}
