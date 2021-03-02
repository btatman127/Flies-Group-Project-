public class CurrentActionSettings {
    private int larva;
    private int buttonType;

    public CurrentActionSettings(int button) {
        this.buttonType = button;
        this.larva = -1;
    }

    public CurrentActionSettings(int button, int larvaNum) {
        this.buttonType = button;
        this.larva = larvaNum;
    }

    public int getLarva() {return larva;}

    public int getButtonType() {return buttonType;}
}
