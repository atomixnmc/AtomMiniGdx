package sg.atom.corex.managers;

import com.google.common.eventbus.Subscribe;
import com.jme3.audio.AudioNode;
import com.jme3.audio.LowPassFilter;
import sg.atom.core.lifecycle.AbstractManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.atom.core.AtomMain;
import sg.atom.corex.entity.ComposableEntity;
import sg.atom.corex.entity.EntityManager.EntityRemovalEvent;
import sg.atom.corex.entity.SpatialEntity;

/**
 *
 * @author cuong.nguyen
 */
public class WorldManager extends AbstractManager {

    public static final Logger logger = LoggerFactory.getLogger(WorldManager.class.getName());
    protected Node worldNode;
    protected Node levelNode;
    //Models and nodes
    protected HashMap<String, Node> nodes;
    //    HashMap<Geometry, Spatial> collisionMap;
    private Geometry boxGeom;
    public Node gizmo = new Node("gizmo");
    public Geometry ground;
    public Geometry gridGeo;
    public Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    public WaterFilter water;
    Material matRock;
    AudioNode waveSounds;
    LowPassFilter underWaterAudioFilter = new LowPassFilter(0.5f, 0.1f);
    LowPassFilter underWaterReverbFilter = new LowPassFilter(0.5f, 0.1f);
    LowPassFilter aboveWaterAudioFilter = new LowPassFilter(1, 1);
    public float time = 0.0f;
    public float waterHeight = 0.0f;
    public float initialWaterHeight = 5f;
    public boolean isUnderWater = false;

    //Settings?
    public boolean isUseLight = true;
    public boolean isUseShadow = true;
    public boolean isUseWater = false;

    public WorldManager(AtomMain app) {
        super(app);
        this.nodes = new HashMap<String, Node>();
        this.worldNode = new Node("WorldNode");
    }

    public void loadLevels() {
    }

    @Override
    public void load() {
        //Load
        if (getApp().isDebugMode()) {
            createGrid(20, 20);
            createGizmo();
        }
        if (isUseLight) {
            setupLights();
        }
        if (isUnderWater) {
            createOcean();
        }
        createEntities();
        attachNodes();
    }

    public void createSkyBox(String path) {
        if (path == null) {
            path = "Textures/Beach/FullskiesSunset0068.dds";
        }
        Spatial sky = SkyFactory.createSky(assetManager, path, false);
        sky.setLocalScale(350);

        worldNode.attachChild(sky);
    }

    public void createLights() {
    }

    public void createMiniMap() {
    }

    public void createEntities() {
    }

    public void createGroupNode(String name) {
        Node groupNode = new Node(name);
        nodes.put(name, groupNode);
        worldNode.attachChild(groupNode);
    }

    public Node getGroupNode(String name) {
        return nodes.get(name);
    }

    public void attachNodes() {
        for (Node node : nodes.values()) {
            worldNode.attachChild(node);
        }
        this.rootNode.attachChild(worldNode);
    }

    public static Vector3f createRandomPos(float x, float y, float z) {
        return new Vector3f(FastMath.nextRandomFloat() * x - x, FastMath.nextRandomFloat() * y, FastMath.nextRandomFloat() * z - z);
    }

    public static Vector2f createRandomPos(float x, float y) {
        return new Vector2f(FastMath.nextRandomFloat() * x - x, FastMath.nextRandomFloat() * y - y);
    }

    public void putEntity(Node parentNode, SpatialEntity entity, Vector2f mapPos) {
        Spatial spatial = entity.getSpatial();
        parentNode.attachChild(spatial);
        spatial.setLocalTranslation(new Vector3f(mapPos.x, 0, mapPos.y));
    }

    public void putEntity(SpatialEntity entity, Vector2f mapPos) {
        putEntity(worldNode, entity, mapPos);
    }

    public void putEntity(SpatialEntity entity, Vector2f mapPos, int state) {
    }

    public void putSpatial(Spatial spatial, Vector3f mapPos) {
        worldNode.attachChild(spatial);
        spatial.setLocalTranslation(new Vector3f(mapPos));
    }

    public void putEntity(Node parentNode, SpatialEntity entity, Vector3f mapPos) {
        Spatial spatial = entity.getSpatial();
        parentNode.attachChild(spatial);
        spatial.setLocalTranslation(new Vector3f(mapPos));
    }

    public void putEntity(SpatialEntity entity, Vector3f mapPos) {
        putEntity(worldNode, entity, mapPos);
    }

    public void putEntity(SpatialEntity entity, Vector3f mapPos, int state) {
    }

    public void putSpatial(Spatial spatial, Vector2f mapPos) {
    }

    @Subscribe
    public void removeEntity(EntityRemovalEvent removalEvent) {
        logger.info("Try to detach entity");
        ComposableEntity e = removalEvent.getEntity();
        if (e instanceof SpatialEntity) {
            ((SpatialEntity) e).getSpatial().removeFromParent();
        }
    }

    public void putArrow(Vector3f pos, Vector3f dir, ColorRGBA color) {
        Arrow arrow = new Arrow(dir);
        arrow.setLineWidth(4); // make arrow thicker

        putShape(gizmo, arrow, color).setLocalTranslation(pos);
        rootNode.attachChild(gizmo);
        gizmo.scale(1);
    }

    public Geometry putShape(Node node, Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("shape", shape);
        Material mat = getColoredMat(color).clone();
        mat.getAdditionalRenderState().setWireframe(true);
        g.setMaterial(mat);
        node.attachChild(g);
        return g;
    }

    public Material getColoredMat(ColorRGBA color) {
        return app.getMaterialManager().getColoredMat(color);
    }

    public void createGizmo() {
        putArrow(Vector3f.ZERO, Vector3f.UNIT_X, ColorRGBA.Red);
        putArrow(Vector3f.ZERO, Vector3f.UNIT_Y, ColorRGBA.Green);
        putArrow(Vector3f.ZERO, Vector3f.UNIT_Z, ColorRGBA.Blue);
    }

    public void createGrid(int gw, int gh) {
        Material mat = getColoredMat(ColorRGBA.DarkGray).clone();

        Grid grid = new Grid(gw + 1, gh + 1, 1);
        gridGeo = new Geometry("Grid", grid);
        gridGeo.setMaterial(mat);
        gridGeo.setLocalTranslation(-gw / 2, 0.1f, -gh / 2);
        rootNode.attachChild(gridGeo);
    }

    public void createFlatGround(int size) {
        Box b = new Box(new Vector3f(0, -0.1f, size / 2), size, 0.01f, size);
        b.scaleTextureCoordinates(new Vector2f(40, 40));
        Material matGround = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        matGround.setTexture("DiffuseMap", grass);
        Geometry ground = new Geometry("Ground", b);

        ground.setMaterial(matGround);
        ground.setShadowMode(RenderQueue.ShadowMode.Receive);
        worldNode.attachChild(ground);
    }

    public void createForest() {
    }

    protected void createOcean() {
        //Water Filter
        water = new WaterFilter(rootNode, lightDir);
        water.setWaterColor(new ColorRGBA().setAsSrgb(0.0078f, 0.3176f, 0.9f, 1.0f));
        water.setDeepWaterColor(new ColorRGBA().setAsSrgb(0.0039f, 0.00196f, 0.145f, 1.0f));
        water.setUnderWaterFogDistance(80);
        water.setWaterTransparency(0.62f);
        water.setFoamIntensity(0.4f);
        water.setFoamHardness(0.3f);
        water.setFoamExistence(new Vector3f(0.8f, 8f, 1f));
        water.setReflectionDisplace(50);
        water.setRefractionConstant(0.25f);
        water.setColorExtinction(new Vector3f(30, 50, 70));
        water.setCausticsIntensity(0.4f);
        water.setWaveScale(0.003f);
        water.setMaxAmplitude(2f);
        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
        water.setRefractionStrength(0.1f);
        water.setWaterHeight(initialWaterHeight);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(water);
        getApp().getViewPort().addProcessor(fpp);

        isUnderWater = getApp().getCamera().getLocation().y < waterHeight;
    }

    public void setupLights() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        getWorldNode().addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White);
        getWorldNode().addLight(ambient);
        
//        PointLight lamp = new PointLight();
//        lamp.setPosition(new Vector3f(0, 40, 0));
//        lamp.setColor(ColorRGBA.White);
//        getWorldNode().addLight(lamp);

        if (isUseShadow) {
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 2);
            dlsf.setLight(sun);
            fpp.addFilter(dlsf);
            app.getViewPort().addProcessor(fpp);
        }
    }

    public Spatial createPlaceHolder(String type) {
        return boxGeom.clone();
    }

    public SpatialEntity getEntity(Geometry target) {
        SpatialEntity result = null;
//        worldNode.depthFirstTraversal(new SceneGraphVisitorAdapter(){
//
//            @Override
//            public void visit(Geometry geom) {
//                super.visit(geom); 
//                
//                
//            }
//            
//        });
        return result;
    }

    public Node getCollisionNode() {
        return worldNode;
    }

    public Spatial getModel(String modelName) {
        return app.getGlobalAssetCache().getModel(modelName);
    }

    public Node getWorldNode() {
        return worldNode;
    }
}
