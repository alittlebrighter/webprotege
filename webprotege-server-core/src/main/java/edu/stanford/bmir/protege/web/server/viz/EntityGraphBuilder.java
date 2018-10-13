package edu.stanford.bmir.protege.web.server.viz;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import edu.stanford.bmir.protege.web.server.renderer.RenderingManager;
import edu.stanford.bmir.protege.web.shared.entity.OWLClassData;
import edu.stanford.bmir.protege.web.shared.entity.OWLEntityData;
import edu.stanford.bmir.protege.web.shared.entity.OWLNamedIndividualData;
import edu.stanford.bmir.protege.web.shared.entity.OWLObjectPropertyData;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 13 Oct 2018
 *
 * Builds a simple graph for an entity.  The graph is rooted at the entity and contains edges to depict
 * ISA links e.g. SubClassOf(:A :B), and other relationship links, e.g. SubClassOf(:A ObjectSomeValuesFrom(:R :B))
 */
public class EntityGraphBuilder {

    @Nonnull
    private final RenderingManager renderingManager;

    @Nonnull
    private final OWLOntology ontology;

    @Inject
    public EntityGraphBuilder(@Nonnull RenderingManager renderingManager,
                              @Nonnull OWLOntology ontology) {
        this.renderingManager = checkNotNull(renderingManager);
        this.ontology = checkNotNull(ontology);
    }

    @Nonnull
    public Graph createGraph(@Nonnull OWLEntity root) {
        LinkedHashSet<Edge> edges = new LinkedHashSet<>();
        createGraph(root, edges, new HashSet<>());
        return Graph.create(ImmutableSet.copyOf(edges));
    }

    private void createGraph(@Nonnull OWLEntity entity,
                             @Nonnull Set<Edge> g,
                             @Nonnull Set<OWLEntity> processed) {
        if(processed.contains(entity)) {
            return;
        }
        processed.add(entity);
        if(entity.isOWLClass()) {
            OWLClass cls = entity.asOWLClass();
            createEdgesForClass(g, processed, cls);
        }
        else if(entity.isOWLNamedIndividual()) {
            OWLNamedIndividual ind = entity.asOWLNamedIndividual();
            createEdgesForIndividual(g, processed, ind);
        }
    }

    private void createEdgesForIndividual(Set<Edge> g, Set<OWLEntity> processed, OWLNamedIndividual individual) {
        OWLNamedIndividualData indvidualData = renderingManager.getRendering(individual);
        ontology.getClassAssertionAxioms(individual)
                .stream()
                .filter(ax -> !ax.getClassExpression().isOWLThing())
                .filter(ax -> !ax.getClassExpression().isAnonymous())
                .forEach(ax -> {
                    OWLClass cls = ax.getClassExpression().asOWLClass();
                    OWLClassData clsData = renderingManager.getRendering(cls);
                    g.add(IsAEdge.get(indvidualData, clsData));
                    createEdgesForClass(g, processed, cls);
                });
        ontology.getObjectPropertyAssertionAxioms(individual)
                .stream()
                .filter(ax -> ax.getObject().isNamed())
                .filter(ax -> !ax.getProperty().isAnonymous())
                .forEach(ax -> {
                    OWLNamedIndividual object = ax.getObject().asOWLNamedIndividual();
                    OWLNamedIndividualData objectData = renderingManager.getRendering(object);
                    OWLObjectPropertyData propertyData = renderingManager.getRendering(ax.getProperty().asOWLObjectProperty());
                    g.add(RelationshipEdge.get(indvidualData, objectData, propertyData));
                    createEdgesForIndividual(g, processed, object);
                });

    }

    private void createEdgesForClass(Set<Edge> g, Set<OWLEntity> processed, OWLClass cls) {
        Stream<OWLSubClassOfAxiom> subClsAx = ontology.getSubClassAxiomsForSubClass(cls).stream();
        Stream<OWLSubClassOfAxiom> defs =
                ontology.getEquivalentClassesAxioms(cls)
                        .stream()
                        .flatMap(ax -> ax.asOWLSubClassOfAxioms().stream());
        Streams.concat(subClsAx, defs)
                .filter(ax -> !ax.getSubClass().isAnonymous())
                .forEach(ax -> createEdgeForSubClassOfAxiom(cls, ax, g, processed));
    }

    private void createEdgeForSubClassOfAxiom(OWLClass subCls, OWLSubClassOfAxiom ax, Set<Edge> edges, Set<OWLEntity> processed) {
        OWLEntityData subClsData = renderingManager.getRendering(subCls);
        ax.getSuperClass().asConjunctSet()
                .stream()
                .filter(c -> !c.isOWLThing())
                .forEach(superClass -> {
                    if(!superClass.isAnonymous()) {
                        OWLClass superCls = superClass.asOWLClass();
                        OWLEntityData superClsData = renderingManager.getRendering(superCls);
                        Edge edge = IsAEdge.get(subClsData, superClsData);
                        edges.add(edge);
                        createGraph(superCls, edges, processed);
                    }
                    else {
                        if(superClass instanceof OWLObjectSomeValuesFrom) {
                            OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) superClass;
                            OWLClassExpression filler = svf.getFiller();
                            if(!filler.isAnonymous()) {
                                OWLClass fillerCls = filler.asOWLClass();
                                OWLClassData fillerClsData = renderingManager.getRendering(fillerCls);
                                OWLObjectProperty prop = svf.getProperty().asOWLObjectProperty();
                                OWLEntityData propData = renderingManager.getRendering(prop);
                                Edge edge = RelationshipEdge.get(subClsData, fillerClsData, propData);
                                edges.add(edge);
                                createGraph(fillerCls, edges, processed);
                            }
                        }
                    }
                });
    }

    private OWLEntityData toEntity(@Nonnull OWLClass cls) {
        return renderingManager.getRendering(cls);
    }
}
