/**
 * 
 */
package edu.uw.sig.ont.fma;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * @author detwiler
 *
 */
public class ReleaseVersionTagger
{
	private OWLOntologyManager ontMan;
	private OWLDataFactory ontDF;
	private OWLOntology ont;
	
	private String ontPath;
	
	public ReleaseVersionTagger()
	{

	}
	
	private void run(String ontPath, String versionNumber)
	{
		// load ont and initiate data structures
		try
		{
			init(ontPath);
		}
		catch (OWLOntologyCreationException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
		
		// set version number
		setVersionNumber(versionNumber);
		
		// save modified ontology
		saveOnt();
	}
	
	private void init(String ontPath) throws OWLOntologyCreationException, IOException
	{
		// to get around a bug in Java
		System.setProperty("jdk.xml.entityExpansionLimit", "0");
		
		this.ontPath = ontPath;
		
		// first load base ontology
		ontMan = OWLManager.createOWLOntologyManager();
		
		// deal with imports
		AutoIRIMapper mapper = new AutoIRIMapper(new File("resource"), true);
		ontMan.addIRIMapper(mapper);
		
		ontDF = ontMan.getOWLDataFactory();
		
		File baseFile = new File(ontPath);
		try
		{
			ontMan.setSilentMissingImportsHandling(true);
			//OWLOntologyLoaderConfiguration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
			ont = ontMan.loadOntologyFromOntologyDocument(baseFile);
		}
		catch (UnloadableImportException e)
		{
			// don't worry about this for now, later we could add AutoIRIMapper
			System.out.println("skipping unloadable import");
		}
	}
	
	private void setVersionNumber(String versionNumber)
	{
		// first set the versionInfo property value
		OWLAnnotationProperty versionInfoProperty = ontDF.getOWLAnnotationProperty(OWLRDFVocabulary.OWL_VERSION_INFO.getIRI());
		OWLLiteral versionLit = ontDF.getOWLLiteral(versionNumber);
		OWLAnnotation versionInfoAnnot = ontDF.getOWLAnnotation(versionInfoProperty, versionLit);
		ontMan.applyChange(new AddOntologyAnnotation(ont, versionInfoAnnot));
		
				
		// note: this presumes that the ontology IRI ends with a '.' followed by a file type suffix (i.e. ".owl")
		OWLOntologyID id = ont.getOntologyID();
		IRI ontIRI = id.getOntologyIRI();
		
		// construct new version IRI
		String ontIRIString = ontIRI.toString();
		int periodInd = ontIRIString.lastIndexOf('.');
		String newVerIRIString = ontIRIString.substring(0,periodInd)+"_"+versionNumber+ontIRIString.substring(periodInd);
		IRI newVerIRI = IRI.create(newVerIRIString);
		
		// create new ontology ID using new version IRI
		OWLOntologyID newID = new OWLOntologyID(ontIRI,newVerIRI);
		
		SetOntologyID versionIDChange = new SetOntologyID(ont, newID);
		ontMan.applyChange(versionIDChange);
	}
	
	private boolean saveOnt()
	{
		try
		{
			File output = new File(ontPath);
			IRI documentIRI = IRI.create(output);
			
			// get current format
			OWLOntologyFormat format = ontMan.getOntologyFormat(ont);
			
			 // Now save a copy to another location in OWL/XML format (i.e. disregard
			// the format that the ontology was loaded in).
			//File f = File.createTempFile("owlapiexample", "example1.xml");
			//IRI documentIRI2 = IRI.create(output);
			ontMan.saveOntology(ont, format, documentIRI);
			
			// Remove the ontology from the manager
			ontMan.removeOntology(ont);
		}
		catch (OWLOntologyStorageException e)
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ReleaseVersionTagger tagger = new ReleaseVersionTagger();
		tagger.run(args[0], args[1]);
	}

}
