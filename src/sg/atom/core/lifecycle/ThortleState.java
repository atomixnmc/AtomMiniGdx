package sg.atom.core.lifecycle;

import com.jme3.app.state.AbstractAppState;

/**
 * Class for controling that all updates are in 60fps.
 *
 * @author mifth
 */
public class ThortleState extends AbstractAppState {

    private long lastFrame = System.nanoTime();
    private double lastPreviousFrame = 0.0;
    private float currentTpf = 0f;
    private boolean update = false;
    
    // It's 60fps.
    private final static double framerate = 1.0 / 60.0;

    public boolean IsUpdate() {
        return update;
    }

    public float getCurrentTpf() {
        return currentTpf;
    }

    public void setCurrentTpf(float currentTpf) {
        this.currentTpf = currentTpf;
    }

    @Override
    public void update(float tpf) {
        // Use our own tpf calculation in case frame rate is
        // running away making this tpf unstable
        long time = System.nanoTime();

        long delta = time - lastFrame;

        double seconds = (delta / 1000000000.0);

        // Clamp frame time to no bigger than a certain amount 60fps
        if (seconds + lastPreviousFrame >= framerate) {
            lastFrame = time;
//            System.out.println(seconds);
            update = true;

            currentTpf = (float) seconds;
            lastPreviousFrame = (seconds + lastPreviousFrame) % framerate;
            

//            System.out.println(currentTpf + "" + tpf);

        } else {
            update = false;
//            System.out.println("Shit  " + tpf);
        }
    }
}
