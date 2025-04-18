import java.security.PrivateKey;
import java.util.ArrayList;

public class SwitchState {
    private boolean state=false;
    private  ArrayList<Observer> List = new ArrayList<Observer>();

    public void ObjAdd(Observer o){
        List.add(o);
    }
    public void remove(Observer o){
        List.remove(o);
    }
    public boolean GetState(){
        return state;
    }
    public void Switch(){
        this.state= !this.state;
        for(Observer o : List){
            Notify(o);
        }
    }
    public void Notify(Observer b){
        b.SetState();
    }
}
