package com.sigmajs;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingLabelSizeTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.EdgeMergeStrategy;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.AppendProcessor;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import uk.ac.ox.oii.sigmaexporter.SigmaExporter;
import uk.ac.ox.oii.sigmaexporter.model.ConfigFile;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Demo {
    public static void main(String[] args) {
        //初始化工作台
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //获取到各个模块以及控制器
        //整个图的边和节点信息模块
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        //预览模块
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        //导入模块
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        //过滤模块
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        //外观展示模块
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();
        //通过节点和边的方式导入
        Container container1 = null, container2 = null;
        try{
            //导入节点csv文件
            File nodeFile = new File("data/input/output_all_csv_node.csv");
            container1 = importController.importFile(nodeFile);
            container1.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);
            container1.getLoader().setAllowAutoNode(false);
            container1.getLoader().setEdgesMergeStrategy(EdgeMergeStrategy.SUM);
            container1.getLoader().setAutoScale(true);

            //导入边csv文件
            File edgeFile = new File("data/input/output_all_csv_edge.csv");
            container2 = importController.importFile(edgeFile);
            container2.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);
            container2.getLoader().setAllowAutoNode(false);
            container2.getLoader().setEdgesMergeStrategy(EdgeMergeStrategy.SUM);
            container2.getLoader().setAutoScale(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //将节点和边导入工作台
        importController.process((org.gephi.io.importer.api.Container) container1, new DefaultProcessor(),workspace);
        //以追加方式导入边
        importController.process((org.gephi.io.importer.api.Container) container2, new AppendProcessor(),workspace);
        //打印信息看图文件是否成功导入
        DirectedGraph graph = graphModel.getDirectedGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());

        //过滤器
        DegreeRangeBuilder.DegreeRangeFilter degreeFilter = new DegreeRangeBuilder.DegreeRangeFilter();
        degreeFilter.init(graph);
        //过滤度小于10的节点
        degreeFilter.setRange(new Range(10, Integer.MAX_VALUE));
        Query query = filterController.createQuery(degreeFilter);
        GraphView view = filterController.filter(query);
        graphModel.setVisibleView(view);

        //查看过滤是否生效
        UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
        System.out.println("After filter:");
        System.out.println("Nodes: " + graphVisible.getNodeCount());
        System.out.println("Edges: " + graphVisible.getEdgeCount());

        //使用YifanHu布局
        YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.setOptimalDistance(200f);
        layout.initAlgo();
        for (int i = 0; i < 100 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }
        layout.endAlgo();
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.execute(graphModel);

        //根据节点度值分配节点颜色
        Function degreeRanking = appearanceModel.getNodeFunction(graph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class);
        RankingElementColorTransformer degreeTransformer = degreeRanking.getTransformer();
        //节点的度由少到多，颜色由color1渐变到color2
        degreeTransformer.setColors(new Color[]{new Color(249, 210, 125), new Color(43, 115, 174)});
        degreeTransformer.setColorPositions(new float[]{0f, 0.1f});
        appearanceController.transform(degreeRanking);

        //分配节点大小
        Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
        RankingNodeSizeTransformer centralityTransformer = centralityRanking.getTransformer();
        centralityTransformer.setMinSize(3);
        centralityTransformer.setMaxSize(10);
        appearanceController.transform(centralityRanking);

        //标签大小
        Function centralityRanking2 = appearanceModel.getNodeFunction(graph, centralityColumn, RankingLabelSizeTransformer.class);
        RankingLabelSizeTransformer labelSizeTransformer = centralityRanking2.getTransformer();
        labelSizeTransformer.setMinSize(1);
        labelSizeTransformer.setMaxSize(3);
        appearanceController.transform(centralityRanking2);

        //预览设置
        model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
        model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 0.1f);
        model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));

        //导出pdf
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("demo.pdf"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //导出web
        SigmaExporter se = new SigmaExporter();
        se.setWorkspace(workspace);
        ConfigFile cf = new ConfigFile();
        cf.setDefaults();
        se.setConfigFile(cf, "data/output/", false);
        se.execute();
    }
}