package org.weasis.core.ui.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.util.MeasurableLayer;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.ui.Messages;
import org.weasis.core.ui.editor.image.Canvas;
import org.weasis.core.ui.editor.image.DefaultView2d;
import org.weasis.core.ui.editor.image.MeasureToolBar;
import org.weasis.core.ui.editor.image.ViewCanvas;
import org.weasis.core.ui.model.graphic.DragGraphic;
import org.weasis.core.ui.model.graphic.Graphic;
import org.weasis.core.ui.model.graphic.GraphicLabel;
import org.weasis.core.ui.model.graphic.GraphicSelectionListener;
import org.weasis.core.ui.model.graphic.imp.AnnotationGraphic;
import org.weasis.core.ui.model.graphic.imp.PixelInfoGraphic;
import org.weasis.core.ui.model.graphic.imp.PointGraphic;
import org.weasis.core.ui.model.graphic.imp.angle.AngleToolGraphic;
import org.weasis.core.ui.model.graphic.imp.angle.CobbAngleToolGraphic;
import org.weasis.core.ui.model.graphic.imp.angle.FourPointsAngleToolGraphic;
import org.weasis.core.ui.model.graphic.imp.angle.OpenAngleToolGraphic;
import org.weasis.core.ui.model.graphic.imp.area.EllipseGraphic;
import org.weasis.core.ui.model.graphic.imp.area.PolygonGraphic;
import org.weasis.core.ui.model.graphic.imp.area.RectangleGraphic;
import org.weasis.core.ui.model.graphic.imp.area.SelectGraphic;
import org.weasis.core.ui.model.graphic.imp.area.ThreePointsCircleGraphic;
import org.weasis.core.ui.model.graphic.imp.line.LineGraphic;
import org.weasis.core.ui.model.graphic.imp.line.LineWithGapGraphic;
import org.weasis.core.ui.model.graphic.imp.line.ParallelLineGraphic;
import org.weasis.core.ui.model.graphic.imp.line.PerpendicularLineGraphic;
import org.weasis.core.ui.model.graphic.imp.line.PolylineGraphic;
import org.weasis.core.ui.model.layer.GraphicLayer;
import org.weasis.core.ui.model.layer.GraphicModelChangeListener;
import org.weasis.core.ui.model.layer.LayerType;
import org.weasis.core.ui.model.layer.imp.DragLayer;
import org.weasis.core.ui.model.utils.imp.DefaultUUID;
import org.weasis.core.ui.util.MouseEventDouble;

@XmlType(propOrder = { "referencedSeries", "layers", "models" })
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractGraphicModel extends DefaultUUID implements GraphicModel {
    private static final long serialVersionUID = 1187916695295007387L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGraphicModel.class);

    private volatile List<ReferencedSeries> referencedSeries;
    private volatile List<GraphicLayer> layers;
    protected volatile List<Graphic> models;

    private final ArrayList<GraphicSelectionListener> selectedGraphicsListeners = new ArrayList<>();
    private final ArrayList<GraphicModelChangeListener> listeners = new ArrayList<>();
    private Boolean changeFireingSuspended = Boolean.FALSE;
    private Graphic createGraphic;

    private Function<Graphic, GraphicLayer> getLayer = g -> g.getLayer();
    private Function<Graphic, DragGraphic> castToDragGraphic = DragGraphic.class::cast;

    private Predicate<Graphic> isLayerVisible = g -> g.getLayer().getVisible();
    private Predicate<Graphic> isGraphicSelected = g -> g.getSelected();

    public AbstractGraphicModel() {
        this(null);
    }

    public AbstractGraphicModel(List<ReferencedSeries> referencedSeries) {
        setReferencedSeries(referencedSeries);
        this.layers = Collections.synchronizedList(new ArrayList<>());
        this.models = Collections.synchronizedList(new ArrayList<>());
    }

    @XmlElementWrapper(name = "graphics")
    @XmlElements({ @XmlElement(name = "point", type = PointGraphic.class),
        @XmlElement(name = "angle", type = AngleToolGraphic.class),
        @XmlElement(name = "annotation", type = AnnotationGraphic.class),
        @XmlElement(name = "pixelInfo", type = PixelInfoGraphic.class),
        @XmlElement(name = "openAngle", type = OpenAngleToolGraphic.class),
        @XmlElement(name = "cobbAngle", type = CobbAngleToolGraphic.class),
        @XmlElement(name = "rectangle", type = RectangleGraphic.class),
        @XmlElement(name = "ellipse", type = EllipseGraphic.class),
        @XmlElement(name = "fourPointsAngle", type = FourPointsAngleToolGraphic.class),
        @XmlElement(name = "line", type = LineGraphic.class),
        @XmlElement(name = "lineWithGap", type = LineWithGapGraphic.class),
        @XmlElement(name = "perpendicularLine", type = PerpendicularLineGraphic.class),
        @XmlElement(name = "parallelLine", type = ParallelLineGraphic.class),
        @XmlElement(name = "polygon", type = PolygonGraphic.class),
        @XmlElement(name = "polyline", type = PolylineGraphic.class),
        @XmlElement(name = "threePointsCircle", type = ThreePointsCircleGraphic.class) })
    @Override
    public List<Graphic> getModels() {
        return models;
    }

    @XmlElementWrapper(name = "layers")
    @XmlElements({ 
        @XmlElement(name = "dragLayer", type = DragLayer.class)
    })
    @Override
    public List<GraphicLayer> getLayers() {
        return layers;
    }

    @XmlElementWrapper(name = "references")
    @XmlElement(name = "series")
    @Override
    public List<ReferencedSeries> getReferencedSeries() {
        return referencedSeries;
    }

    @Override
    public void setReferencedSeries(List<ReferencedSeries> referencedSeries) {
        if (referencedSeries != null && !referencedSeries.getClass().getSimpleName().startsWith("Synchronized")) {
            this.referencedSeries = Collections.synchronizedList(referencedSeries);
        }
        this.referencedSeries =
            Optional.ofNullable(referencedSeries).orElse(Collections.synchronizedList(new ArrayList<>()));
    }

    @Override
    public void setModels(List<Graphic> models) {
        if (models != null) {
            this.models = Collections.synchronizedList(models);
            this.layers = Collections.synchronizedList(getLayerlist());
        }
    }

    @Override
    public void addGraphic(Graphic graphic) {
        if (graphic != null) {
            GraphicLayer layer = graphic.getLayer();
            if (layer == null) {
                layer = findLayerByType(graphic.getLayerType()).orElse(new DragLayer(graphic.getLayerType()));
                graphic.setLayer(layer);
            }
            if (!layers.contains(layer)) {
                layers.add(layer);
            }
            models.add(graphic);
            LOGGER.trace("add graphic {})", graphic);
        }
    }

    @Override
    public void removeGraphic(Graphic graphic) {
        if (graphic != null) {
            GraphicLayer layer = graphic.getLayer();
            models.remove(graphic);
            graphic.removeAllPropertyChangeListener();
            graphic.setLayer(null);
            LOGGER.trace("romove graphic({})", graphic);

            if (layer != null) {
                boolean layerExist = false;
                synchronized (models) {
                    for (Graphic g : models) {
                        if (g.getLayer().getUuid().equals(layer.getUuid())) {
                            layerExist = true;
                            break;
                        }
                    }

                }
                if (!layerExist) {
                    layers.remove(layer);
                }
            }
        }
    }

    private List<GraphicLayer> getLayerlist() {
        return models.parallelStream().map(getLayer).distinct().collect(Collectors.toList());
    }

    @Override
    public void addGraphicChangeHandler(PropertyChangeListener graphicsChangeHandler) {
        models.forEach(g -> g.addPropertyChangeListener(graphicsChangeHandler));
    }

    @Override
    public void removeGraphicChangeHandler(PropertyChangeListener graphicsChangeHandler) {
        models.forEach(g -> g.removePropertyChangeListener(graphicsChangeHandler));
    }

    @Override
    public void updateLabels(Object source, ViewCanvas<? extends ImageElement> view) {
        models.forEach(g -> g.updateLabel(source, view));
    }

    @Override
    public Optional<GraphicLayer> findLayerByType(LayerType type) {
        Objects.requireNonNull(type);
        return layers.stream().filter(isLayerTypeEquals(type)).findFirst();
    }

    @Override
    public List<GraphicLayer> groupLayerByType() {
        if (models.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GraphicLayer> layers = new ArrayList<>();
        synchronized (models) {
            for (Graphic g : models) {
                LayerType type = g.getLayer().getType();

                boolean notInGroup = true;
                for (GraphicLayer glayer : layers) {
                    if (Objects.equals(glayer.getType(), type)) {
                        notInGroup = false;
                        break;
                    }
                }

                if (notInGroup) {
                    layers.add(g.getLayer());
                }
            }
        }

        return layers;
    }

    @Override
    public void deleteByLayerType(LayerType type) {
        Objects.requireNonNull(type);
        LOGGER.trace("deleteByLayerType(type => " + type + ")");

        synchronized (models) {
            for (Graphic g : models) {
                if (g.getLayer().getType().equals(type)) {
                    g.removeAllPropertyChangeListener();
                    g.setLayer(null);
                }
            }
            models.removeIf(isModelLayerTypeEquals(type));
            layers.removeIf(l -> Objects.equals(l.getType(), type));
        }

        LOGGER.trace(managerStatus());
    }

    @Override
    public List<Graphic> getAllGraphics() {
        return models.stream().filter(isLayerVisible).collect(Collectors.toList());
    }

    @Override
    public List<Graphic> getSelectedAllGraphicsIntersecting(Rectangle rectangle, AffineTransform transform) {

        ArrayList<Graphic> selectedGraphicList = new ArrayList<>();
        if (rectangle != null) {
            synchronized (models) {
                for (int i = models.size() - 1; i >= 0; i--) {
                    Graphic graphic = models.get(i);
                    GraphicLayer layer = graphic.getLayer();
                    if (layer.getVisible() && !layer.getLocked()) {

                        Rectangle graphBounds = graphic.getBounds(transform);

                        if (graphBounds != null && graphBounds.intersects(rectangle)) {
                            Area selectionArea = graphic.getArea(transform);

                            if (selectionArea != null && selectionArea.intersects(rectangle)) {
                                selectedGraphicList.add(graphic);
                                continue;
                            }
                        }

                        GraphicLabel graphicLabel = graphic.getGraphicLabel();
                        if (graphic.getLabelVisible() && graphicLabel != null && graphicLabel.getLabels() != null) {
                            Area selectionArea = graphicLabel.getArea(transform);

                            if (selectionArea != null && selectionArea.intersects(rectangle)) {
                                selectedGraphicList.add(graphic);
                            }
                        }
                    }
                }
            }
        }
        return selectedGraphicList;
    }

    @Override
    public List<Graphic> getSelectedAllGraphicsIntersecting(Rectangle rectangle, AffineTransform transform,
        boolean onlyFrontGraphic) {
        ArrayList<Graphic> selectedGraphicList = new ArrayList<>();
        if (rectangle != null) {
            synchronized (models) {
                for (int i = models.size() - 1; i >= 0; i--) {
                    Graphic graphic = models.get(i);
                    GraphicLayer layer = graphic.getLayer();
                    if (layer.getVisible() && !layer.getLocked()) {

                        List<Area> selectedAreaList = new ArrayList<>();

                        Area selectedArea = null;

                        Rectangle selectionBounds = graphic.getRepaintBounds(transform);
                        if (selectionBounds != null && selectionBounds.intersects(rectangle)) {
                            selectedArea = graphic.getArea(transform);
                        }

                        GraphicLabel graphicLabel = graphic.getGraphicLabel();
                        if (graphicLabel != null && graphicLabel.getLabels() != null) {
                            Area labelArea = graphicLabel.getArea(transform);
                            if (labelArea != null) {
                                if (selectedArea != null) {
                                    selectedArea.add(labelArea);
                                } else if (labelArea.intersects(rectangle)) {
                                    selectedArea = graphic.getArea(transform);
                                    selectedArea.add(labelArea);
                                }
                            }
                        }

                        if (selectedArea != null) {
                            if (onlyFrontGraphic) {
                                for (Area area : selectedAreaList) {
                                    selectedArea.subtract(area);// subtract any areas from front graphics
                                                                // already selected
                                }
                            }
                            if (selectedArea.intersects(rectangle)) {
                                selectedAreaList.add(selectedArea);
                                selectedGraphicList.add(graphic);
                            }
                        }
                    }
                }
            }
        }
        return selectedGraphicList;
    }

    /**
     * @param mouseevent
     * @return first selected graphic intersecting if exist, otherwise simply first graphic intersecting, or null
     */
    @Override
    public Optional<Graphic> getFirstGraphicIntersecting(MouseEventDouble mouseEvent) {
        final Point2D mousePt = mouseEvent.getImageCoordinates();
        Graphic firstSelectedGraph = null;
        synchronized (models) {
            for (int i = models.size() - 1; i >= 0; i--) {
                Graphic g = models.get(i);
                GraphicLayer l = g.getLayer();
                if (l.getVisible() && !l.getLocked()) {
                    if (g.isOnGraphicLabel(mouseEvent)) {
                        if (g.getSelected()) {
                            return Optional.of(g);
                        } else if (firstSelectedGraph == null) {
                            firstSelectedGraph = g;
                        }
                    }

                    // Improve speed by checking if mousePoint is inside repaintBound before checking if inside Area
                    Rectangle2D repaintBound = g.getRepaintBounds(mouseEvent);
                    if (repaintBound != null && repaintBound.contains(mousePt)) {
                        if ((g.getHandlePointIndex(mouseEvent) >= 0) || (g.getArea(mouseEvent).contains(mousePt))) {
                            if (g.getSelected()) {
                                return Optional.of(g);
                            } else if (firstSelectedGraph == null) {
                                firstSelectedGraph = g;
                            }
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(firstSelectedGraph);
    }

    // @Override
    // public List<Graphic> getGraphicsBoundsInArea(Rectangle rect) {
    // List<Graphic> arraylist = new ArrayList<>();
    // if (graphics != null && rect != null) {
    // for (int j = graphics.list.size() - 1; j >= 0; j--) {
    // Graphic graphic = graphics.list.get(j);
    // Rectangle2D graphicBounds = graphic.getRepaintBounds(getAffineTransform());
    // if (graphicBounds != null && graphicBounds.intersects(rect)) {
    // arraylist.add(graphic);
    // }
    // }
    // }
    // return arraylist;
    // }

    // @Override
    // public AbstractDragGraphic getGraphicContainPoint(MouseEventDouble mouseEvt) {
    // final Point2D mousePt = mouseEvt.getImageCoordinates();
    //
    // if (graphics != null && mousePt != null) {
    //
    // for (int j = graphics.list.size() - 1; j >= 0; j--) {
    // if (graphics.list.get(j) instanceof AbstractDragGraphic) {
    //
    // AbstractDragGraphic dragGraph = (AbstractDragGraphic) graphics.list.get(j);
    //
    // if (dragGraph.isOnGraphicLabel(mouseEvt)) {
    // return dragGraph;
    // }
    //
    // // Improve speed by checking if mousePoint is inside repaintBound before checking if inside Area
    // Rectangle2D repaintBound = dragGraph.getRepaintBounds(mouseEvt);
    // if (repaintBound != null && repaintBound.contains(mousePt)) {
    // if ((dragGraph.getHandlePointIndex(mouseEvt) >= 0)
    // || (dragGraph.getArea(mouseEvt).contains(mousePt))) {
    // return dragGraph;
    // }
    // }
    // }
    // }
    // }
    // return null;
    // }

    @Override
    public List<DragGraphic> getSelectedDragableGraphics() {
        return models.stream().filter(isGraphicSelected).filter(DragGraphic.class::isInstance)
            .map(castToDragGraphic).collect(Collectors.toList());
    }

    @Override
    public List<Graphic> getSelectedGraphics() {
        return models.stream().filter(isGraphicSelected).collect(Collectors.toList());
    }

    @Override
    public Optional<SelectGraphic> getSelectGraphic() {
        return models.stream().filter(g -> g instanceof SelectGraphic).map(SelectGraphic.class::cast).findFirst();
    }

    @Override
    public void setSelectedGraphic(List<Graphic> graphicList) {
        synchronized (models) {
            for (Graphic g : models) {
                g.setSelected(false);
            }
        }

        if (graphicList != null) {
            for (Graphic g : graphicList) {
                g.setSelected(true);
            }
        }
    }

    @Override
    public void setSelectedAllGraphics() {
        setSelectedGraphic(getAllGraphics());
    }

    @Override
    public void deleteSelectedGraphics(Canvas canvas, Boolean warningMessage) {
        List<Graphic> list = getSelectedGraphics();
        if (!list.isEmpty()) {
            int response = 0;
            if (warningMessage) {
                response = JOptionPane.showConfirmDialog(canvas.getJComponent(),
                    String.format(Messages.getString("AbstractLayerModel.del_conf"), list.size()),
                    Messages.getString("AbstractLayerModel.del_graphs"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            }
            if (Objects.equals(response, 0)) {
                list.forEach(g -> g.fireRemoveAction());
                canvas.getJComponent().repaint();
            }
        }
    }

    @Override
    public void clear() {
        models.clear();
    }

    private String managerStatus() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + "[ " + getUuid() + "]");
        sb.append("\n\t-size: " + models.size());
        sb.append("\n");
        models.stream().forEachOrdered(m -> sb.append("\n\t -> " + m.toString()));

        return sb.toString();
    }

    @Override
    public void fireGraphicsSelectionChanged(MeasurableLayer layer) {
        selectedGraphicsListeners.forEach(gl -> gl.handle(getSelectedGraphics(), layer));
    }

    @Override
    public void draw(Graphics2D g2d, AffineTransform transform, AffineTransform inverseTransform,
        Rectangle2D viewClip) {
        // Get the visible view in real coordinates, note only Sun g2d return consistent clip area with offset
        Shape area = inverseTransform.createTransformedShape(viewClip == null ? g2d.getClipBounds() : viewClip);
        Rectangle2D bound = area.getBounds2D();

        g2d.translate(0.5, 0.5);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, DefaultView2d.antialiasingOn);
        // g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        models.forEach(g -> applyPaint(g, g2d, transform, bound));
        // g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, DefaultView2d.antialiasingOff);
        g2d.translate(-0.5, -0.5);
    }

    private static void applyPaint(Graphic graphic, Graphics2D g2d, AffineTransform transform,
        Rectangle2D bounds) {
        if (graphic.getLayer().getVisible()) {
            if (bounds != null) {
                Rectangle repaintBounds = graphic.getRepaintBounds(transform);
                if (repaintBounds != null && repaintBounds.intersects(bounds)) {
                    graphic.paint(g2d, transform);
                } else {
                    GraphicLabel graphicLabel = graphic.getGraphicLabel();
                    if (graphicLabel != null && graphicLabel.getLabels() != null) {
                        Rectangle2D labelBounds = graphicLabel.getBounds(transform);
                        if (labelBounds.intersects(bounds)) {
                            graphic.paintLabel(g2d, transform);
                        }
                    }
                }
            } else { // convention is when bounds equals null graphic is repaint
                graphic.paint(g2d, transform);
                graphic.paintLabel(g2d, transform);
            }
        }
    }

    @Override
    public void addGraphicSelectionListener(GraphicSelectionListener listener) {
        if (Objects.nonNull(listener) && !selectedGraphicsListeners.contains(listener)) {
            selectedGraphicsListeners.add(listener);
        }
    }

    @Override
    public void removeGraphicSelectionListener(GraphicSelectionListener listener) {
        if (Objects.nonNull(listener)) {
            selectedGraphicsListeners.remove(listener);
        }
    }

    @Override
    public GraphicModelChangeListener[] getChangeListeners() {
        return listeners.toArray(new GraphicModelChangeListener[listeners.size()]);
    }

    @Override
    public void addChangeListener(GraphicModelChangeListener listener) {
        if (Objects.nonNull(listener) && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(GraphicModelChangeListener listener) {
        Optional.ofNullable(listener).ifPresent(listeners::remove);
    }

    @Override
    public void fireChanged() {
        if (!changeFireingSuspended) {
            listeners.stream().forEach(l -> l.handleModelChanged(this));
        }
    }

    @Override
    public Boolean isChangeFireingSuspended() {
        return changeFireingSuspended;
    }

    @Override
    public void setChangeFireingSuspended(Boolean change) {
        this.changeFireingSuspended = Optional.ofNullable(change).orElse(Boolean.FALSE);
    }

    @Override
    public void dispose() {
        listeners.clear();
        selectedGraphicsListeners.clear();
    }

    @Override
    public int getLayerCount() {
        return models.stream().collect(Collectors.groupingBy(getLayer)).size();
    }

    @Override
    public void setCreateGraphic(Graphic graphic) {
        createGraphic = graphic;
    }

    @Override
    public Graphic getCreateGraphic() {
        return createGraphic;
    }

    @Override
    public List<GraphicSelectionListener> getGraphicSelectionListeners() {
        return selectedGraphicsListeners;
    }

    public static Graphic drawFromCurrentGraphic(ViewCanvas canvas) {
        Objects.requireNonNull(canvas);
        GraphicModel modelList = canvas.getGraphicManager();
        Objects.requireNonNull(modelList);
    
        Graphic newGraphic =
            Optional.ofNullable(modelList.getCreateGraphic()).orElse(MeasureToolBar.selectionGraphic);
        GraphicLayer layer = canvas.getGraphicManager().findLayerByType(newGraphic.getLayerType())
            .orElse(new DragLayer(newGraphic.getLayerType()));
    
        if (!layer.getVisible() || !(Boolean) canvas.getActionValue(ActionW.DRAW.cmd())) {
            JOptionPane.showMessageDialog(canvas.getJComponent(), Messages.getString("AbstractLayerModel.msg_not_vis"), //$NON-NLS-1$
                Messages.getString("AbstractLayerModel.draw"), //$NON-NLS-1$
                JOptionPane.ERROR_MESSAGE);
            return null;
        } else {
            Graphic graph = newGraphic.copy();
            graph.updateLabel(Boolean.TRUE, canvas);
            graph.addPropertyChangeListener(canvas.getGraphicsChangeHandler());
            graph.setLayer(layer);
            canvas.getGraphicManager().addGraphic(graph);
            return graph;
        }
    }

    public static void addGraphicToModel(ViewCanvas canvas, GraphicLayer layer, Graphic graphic) {
        GraphicModel gm = canvas.getGraphicManager();
        graphic.updateLabel(Boolean.TRUE, canvas);
        graphic.addPropertyChangeListener(canvas.getGraphicsChangeHandler());
        if (layer == null) {
            layer =  getOrBuildLayer(canvas, graphic.getLayerType());
        }
        graphic.setLayer(layer);
        gm.addGraphic(graphic);
    }

    public static void addGraphicToModel(ViewCanvas canvas, Graphic graphic) {
        AbstractGraphicModel.addGraphicToModel(canvas, null, graphic);
    }

    public static GraphicLayer getOrBuildLayer(ViewCanvas canvas, LayerType layerType) {
        return canvas.getGraphicManager().findLayerByType(layerType).orElse(new DragLayer(layerType));
    }

    private static Predicate<GraphicLayer> isLayerTypeEquals(LayerType type) {
        return layer -> Objects.equals(layer.getType(), type);
    }

    private static Predicate<Graphic> isModelLayerTypeEquals(LayerType type) {
        return g -> Objects.equals(g.getLayer().getType(), type);
    }

}
