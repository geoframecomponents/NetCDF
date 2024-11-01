/*
 * GNU GPL v3 License
 *
 * Copyright 2018 Niccolo` Tubini
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.geoframe.blogpsot.netcdf.monodimensionalproblemtimedependent;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Map.Entry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Unit;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

@Description("This class writes a NetCDF with GEOSPACE outputs. Before writing, outputs are stored in a buffer writer"
		+ " and as simulation is ended they are written in a NetCDF file.")
@Documentation("")
@Author(name = "Concetta D'Amato, Niccolo' Tubini, Riccardo Rigon", contact = "concettadamato94@gmail.com")
@Keywords("Hydrology, solute transport equation, advection-dispersion")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")


public class WriteNetCDFGEOSPACESoluteADE1DDouble {

	@Description()
	@In
	@Unit ()
	public String timeUnits = "Minutes since 01/01/1970 00:00:00 UTC";
	
	@Description("Time zone used to convert dates in UNIX time")
	@In
	@Unit()
	public String timeZone = "UTC";

	@Description()
	@In
	@Unit ()
	public LinkedHashMap<String,ArrayList<double[]>> variables;

	@Description()
	@In
	@Unit ()
	public double[] spatialCoordinate;

	@Description()
	@In
	@Unit ()
	public double[] dualSpatialCoordinate;

	@Description("Dimension of each control volume.")
	@In
	@Unit ()
	public double[] controlVolume;
	
	@Description("Initial condition for water suction.")
	@In
	@Unit ()
	public double[] psiIC;
	
	@Description("Initial condition for concentration profile.")
	@In
	@Unit ()
	public double[] concentrationIC;
	
	@Description("Temperature profile.")
	@In
	@Unit ()
	public double[] temperature;
	
	@Description("Initial condition for root profile.")
	@In
	@Unit ()
	public double[] rootIC;
	
	@In
	public int writeFrequency = 1;

	@Description()
	@In
	@Unit ()
	public String fileName;

	@Description("Brief descritpion of the problem")
	@In
	@Unit ()
	public String briefDescritpion;

	@In
	public String topRichardsBC = " ";
	@In
	public String bottomRichardsBC = " ";
	@In
	public String pathRichardsTopBC = " ";
	@In
	public String pathRichardsBottomBC = " ";
	@In
	public String pathGrid = " ";
	@In
	public String timeDelta = " ";
	@In
	public String swrcModel = " ";
	@In
	public String soilHydraulicConductivityModel = " ";
	@In
	public String interfaceConductivityModel = " ";
	
	@In
	public String topSoluteBC = " ";
	@In
	public String bottomSoluteBC = " ";
	@In
	public String pathSoluteTopBC = " ";
	@In
	public String pathSoluteBottomBC = " ";
	@In
	public String interfaceDispersionCoefficientModel = " ";


	@Description("Boolean variable to print output file only at the end of the simulation")
	@In
	@Unit ()
	public boolean doProcess;
	
	@Description("Maximum allowed file size")
	@In
	@Unit ()
	public double fileSizeMax = 10000;
	
	@Description("Name of the variables to save")
	@In
	@Unit ()
	public String [] outVariables = new String[]{""};

	double[] tempVariable;
	Iterator it;
	DateFormat dateFormat;
	Date date = null;
	String fileNameToSave;
	String filename;
	NetcdfFileWriter dataFile;
	int KMAX;
	int DUALKMAX;
	int NREC;
	int[] origin;
	int[] dual_origin;
	int[] time_origin;
	int origin_counter;
	int i;
	int fileNumber = 0;
	double fileSizeMB;
	Dimension kDim;
	Dimension dualKDim;
	Dimension timeDim;
	D1 depth;
	D1 dualDepth;
	Array times;
	String dims;
	List<String> outVariablesList;

	Variable timeVar;
	Variable depthVar;
	Variable dualDepthVar;
	Variable psiVar;
	Variable psiICVar;
	Variable rootDensityICVar;
	Variable temperatureICVar;
	Variable thetaVar;
	Variable darcyVelocitiesVar;
	Variable darcyVelocitiesCapillaryVar;
	Variable darcyVelocitiesGravityVar;
	Variable poreVelocitiesVar;
	Variable celerityVar;
	Variable kinematicRatioVar;
	Variable ETsVar; // i.e. transpired stressed water
	
	Variable concentrationICVar;
	Variable concentrationsVar;
	Variable errorWaterVolumeConcentrationVar;

	Variable waterVolumeConcentrationsVar;
	Variable averageSoluteConcentrationVar;
	Variable averageWaterVolumeSoluteConcentrationVar;
	Variable soluteSourcesSinksTermVar;
	Variable sumSoluteSourceSinksTermVar;
	Variable timeVariationsWConcentrationVar;
	Variable dispersionSoluteFluxesVar;
	Variable advectionSoluteFluxesVar;
	
	Variable errorVar;
	Variable topBCVar;
	Variable bottomBCVar;
	Variable runOffVar;
	Variable controlVolumeVar;
	Variable waterVolumeVar;
	Variable stressWatersVar;
	Variable stressWaterVar;
	Variable evaporationStressWaterVar;
	Variable stressSunVar;
	Variable stressShadeVar;
	Variable stressedETsVar;
	Variable rootDensityVar;
	Variable stressedTsVar;
	Variable stressedEsVar;

	ArrayDouble.D1 dataPsiIC;
	ArrayDouble.D1 dataTemperatureIC;
	ArrayDouble.D1 dataRootDensityIC;
	
	ArrayDouble.D1 dataConcentrationIC;
	ArrayDouble.D1 dataErrorWaterVolumeConcentration;
	ArrayDouble.D1 dataControlVolume;
	ArrayDouble.D1 dataAverageSoluteConcentration;
	ArrayDouble.D1 dataAverageWaterVolumeSoluteConcentration;
	ArrayDouble.D1 dataSumSoluteSourceSinksTerm;
	
	ArrayDouble.D1 dataError;
	ArrayDouble.D1 dataTopBC;
	ArrayDouble.D1 dataBottomBC;
	ArrayDouble.D1 dataRunOff;
	ArrayDouble.D1 dataStressWater;
	ArrayDouble.D1 dataEvaporationStressWater;
	ArrayDouble.D1 dataStressSun;
	ArrayDouble.D1 dataStressShade;
	
	ArrayDouble.D2 dataPsi;
	ArrayDouble.D2 dataTheta;
	ArrayDouble.D2 dataDarcyVelocities;
	ArrayDouble.D2 dataDarcyVelocitiesCapillary;
	ArrayDouble.D2 dataDarcyVelocitiesGravity;
	ArrayDouble.D2 dataPoreVelocities;
	ArrayDouble.D2 dataCelerity;
	ArrayDouble.D2 dataKinematicRatio;
	ArrayDouble.D2 dataETs;
	ArrayDouble.D2 dataConcentrations;
	ArrayDouble.D2 dataWaterVolumeConcentrations;
	ArrayDouble.D2 dataTimeVariationsWConcentration;
	ArrayDouble.D2 dataDispersionSoluteFluxes;
	ArrayDouble.D2 dataAdvectionSoluteFluxes;
	ArrayDouble.D2 dataSoluteSourceSinksTerm;
	ArrayDouble.D2 dataWaterVolume;
	ArrayDouble.D2 dataStressWaters; //stress water for each control volume 
	ArrayDouble.D2 dataStressedETs;
	ArrayDouble.D2 dataRootDensity;
	ArrayDouble.D2 dataStressedTs;
	ArrayDouble.D2 dataStressedEs;

	
	int step = 0;
	int stepCreation = 0;

	@Execute
	public void writeNetCDF() throws IOException {


		/*
		 * Create a new file
		 */
		if(stepCreation == 0) {
			
			origin_counter = 0;
			outVariablesList = Arrays.asList(outVariables);
			
			//			System.out.println("WriterNetCDF step:" + step);
			KMAX = spatialCoordinate.length;
			DUALKMAX = dualSpatialCoordinate.length;
			//			NREC = myVariables.keySet().size();

			dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
			date = null;

			origin = new int[]{0, 0};
			dual_origin = new int[]{0, 0};
			time_origin = new int[]{0};

			dataFile = null;

			fileNameToSave = fileName.substring(0,fileName.length()-3) + '_' + String.format("%04d", fileNumber) + fileName.substring(fileName.length()-3,fileName.length());

			try {
				// Create new netcdf-3 file with the given filename
				dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileNameToSave);
				// add a general attribute describing the problem and containing other relevant information for the user
				dataFile.addGroupAttribute(null, new Attribute("Description of the problem",briefDescritpion));
				/*dataFile.addGroupAttribute(null, new Attribute("Top boundary condition",topBC));
				dataFile.addGroupAttribute(null, new Attribute("Bottom boundary condition",bottomBC));
				dataFile.addGroupAttribute(null, new Attribute("path top boundary condition",pathTopBC));
				dataFile.addGroupAttribute(null, new Attribute("path bottom boundary condition",pathBottomBC));*/
				
				dataFile.addGroupAttribute(null, new Attribute("Top boundary condition for Richards equation",topRichardsBC));
				dataFile.addGroupAttribute(null, new Attribute("Bottom boundary condition for Richards equation",bottomRichardsBC));
				dataFile.addGroupAttribute(null, new Attribute("path top boundary condition for Richards equation",pathRichardsTopBC));
				dataFile.addGroupAttribute(null, new Attribute("path bottom boundary condition for Richards equation",pathRichardsBottomBC));
				
				dataFile.addGroupAttribute(null, new Attribute("path grid",pathGrid));			
				dataFile.addGroupAttribute(null, new Attribute("time delta",timeDelta));
				dataFile.addGroupAttribute(null, new Attribute("swrc model",swrcModel));
				dataFile.addGroupAttribute(null, new Attribute("soil hydraulic conductivity model",soilHydraulicConductivityModel));
				dataFile.addGroupAttribute(null, new Attribute("interface conductivity model",interfaceConductivityModel));
				dataFile.addGroupAttribute(null, new Attribute("Top boundary condition for solute equation",topSoluteBC));
				dataFile.addGroupAttribute(null, new Attribute("Bottom boundary condition for solute equation",bottomSoluteBC));
				dataFile.addGroupAttribute(null, new Attribute("path top boundary condition for solute equation",pathSoluteTopBC));
				dataFile.addGroupAttribute(null, new Attribute("path bottom boundary condition for heat equation",pathSoluteBottomBC));
				dataFile.addGroupAttribute(null, new Attribute("interface dispersion coefficient model",interfaceDispersionCoefficientModel));
				
				//add dimensions  where time dimension is unlimit
				// the spatial dimension is defined using just the indexes 
				kDim = dataFile.addDimension(null, "depth", KMAX);
				dualKDim = dataFile.addDimension(null, "dualDepth", DUALKMAX);
				timeDim = dataFile.addUnlimitedDimension("time");

				// Define the coordinate variables.
				depthVar = dataFile.addVariable(null, "depth", DataType.DOUBLE, "depth");
				dualDepthVar = dataFile.addVariable(null, "dualDepth", DataType.DOUBLE, "dualDepth");
				timeVar = dataFile.addVariable(null, "time", DataType.INT, "time");

				// Define units attributes for data variables.
				// Define units attributes for data variables.
				dataFile.addVariableAttribute(timeVar, new Attribute("units", timeUnits));
				dataFile.addVariableAttribute(timeVar, new Attribute("long_name", "Time."));

				dataFile.addVariableAttribute(depthVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(depthVar, new Attribute("long_name", "Soil depth."));

				dataFile.addVariableAttribute(dualDepthVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(dualDepthVar, new Attribute("long_name", "Dual soil depth."));

				// Define the netCDF variables and their attributes.
				String dims = "time depth";
				String dualDims = "time dualDepth";

				psiVar = dataFile.addVariable(null, "psi", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(psiVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(psiVar, new Attribute("long_name", "Water suction."));
				
				psiICVar = dataFile.addVariable(null, "psiIC", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(psiICVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(psiICVar, new Attribute("long_name", "Initial condition for water suction."));
				
				rootDensityICVar = dataFile.addVariable(null, "rootDensityIC", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(rootDensityICVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(rootDensityICVar, new Attribute("long_name", "Initial condition for root depth."));

				temperatureICVar = dataFile.addVariable(null, "Temperature", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(temperatureICVar, new Attribute("units", "K"));
				dataFile.addVariableAttribute(temperatureICVar, new Attribute("long_name", "Temperature."));
				
				thetaVar = dataFile.addVariable(null, "theta", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(thetaVar, new Attribute("units", " "));
				dataFile.addVariableAttribute(thetaVar, new Attribute("long_name", "theta for within soil and water depth."));
				
				waterVolumeVar  = dataFile.addVariable(null, "waterVolume", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(waterVolumeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(waterVolumeVar, new Attribute("long_name", "Water volume in each control volume"));
				
				if (outVariablesList.contains("darcyVelocity") || outVariablesList.contains("all")) {
					darcyVelocitiesVar = dataFile.addVariable(null, "darcyVelocity", DataType.DOUBLE, dualDims);
					dataFile.addVariableAttribute(darcyVelocitiesVar, new Attribute("units", "m/s"));
					dataFile.addVariableAttribute(darcyVelocitiesVar, new Attribute("long_name", "Darcy velocity."));
				}
				
				if (outVariablesList.contains("darcyVelocityCapillary") || outVariablesList.contains("all")) {
					darcyVelocitiesCapillaryVar = dataFile.addVariable(null, "darcyVelocityCapillary", DataType.DOUBLE, dualDims);
					dataFile.addVariableAttribute(darcyVelocitiesCapillaryVar, new Attribute("units", "m/s"));
					dataFile.addVariableAttribute(darcyVelocitiesCapillaryVar, new Attribute("long_name", "Darcy velocity due to the gradient of capillary forces."));
				}
				
				if (outVariablesList.contains("darcyVelocityGravity") || outVariablesList.contains("all")) {
					darcyVelocitiesGravityVar = dataFile.addVariable(null, "darcyVelocity_gravity", DataType.DOUBLE, dualDims);
					dataFile.addVariableAttribute(darcyVelocitiesGravityVar, new Attribute("units", "m/s"));
					dataFile.addVariableAttribute(darcyVelocitiesGravityVar, new Attribute("long_name", "Darcy velocities due to the gradient of gravity."));
				}
				
				if (outVariablesList.contains("poreVelocity") || outVariablesList.contains("all")) {
					poreVelocitiesVar = dataFile.addVariable(null, "poreVelocity", DataType.DOUBLE, dualDims);
					dataFile.addVariableAttribute(poreVelocitiesVar, new Attribute("units", "m/s"));
					dataFile.addVariableAttribute(poreVelocitiesVar, new Attribute("long_name", "Pore velocities, ratio between the Darcy velocities and porosity."));
				}
				
				if (outVariablesList.contains("celerity") || outVariablesList.contains("all")) {
					celerityVar = dataFile.addVariable(null, "celerities", DataType.DOUBLE, dualDims);
					dataFile.addVariableAttribute(celerityVar, new Attribute("units", "m/s"));
					dataFile.addVariableAttribute(celerityVar, new Attribute("long_name", "Celerity of the pressure wave (Rasmussen et al. 2000"));
				}
				
				if (outVariablesList.contains("kinematicRatio") || outVariablesList.contains("all")) {
					kinematicRatioVar  = dataFile.addVariable(null, "kinematicRatio", DataType.DOUBLE, dualDims);
					dataFile.addVariableAttribute(kinematicRatioVar, new Attribute("units", "-"));
					dataFile.addVariableAttribute(kinematicRatioVar, new Attribute("long_name", "Kinematic ratio (Rasmussen et al. 2000)"));
				}
				
				if (outVariablesList.contains("boundaryConditions") || outVariablesList.contains("all")) {
					
					topBCVar  = dataFile.addVariable(null, "topBC", DataType.DOUBLE, "time");
					dataFile.addVariableAttribute(topBCVar, new Attribute("units", "mm"));                  
					dataFile.addVariableAttribute(topBCVar, new Attribute("long_name", "Rainfall heights")); 
					
					bottomBCVar = dataFile.addVariable(null, "bottomBC", DataType.DOUBLE, "time");
					dataFile.addVariableAttribute(bottomBCVar, new Attribute("units", ""));                
					dataFile.addVariableAttribute(bottomBCVar, new Attribute("long_name", "bottomBC")); 
				}

				ETsVar  = dataFile.addVariable(null, "ets", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(ETsVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(ETsVar, new Attribute("long_name", "Transpired stressed water in Richards"));
				
				concentrationICVar  = dataFile.addVariable(null, "concentrationIC", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(concentrationICVar, new Attribute("units", "ML-3"));
				dataFile.addVariableAttribute(concentrationICVar, new Attribute("long_name", "Initial condition for solute concentration."));

				concentrationsVar  = dataFile.addVariable(null, "concentrations", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(concentrationsVar, new Attribute("units", "ML-3"));
				dataFile.addVariableAttribute(concentrationsVar, new Attribute("long_name", "Solute concentration in each control volume."));
				
				errorWaterVolumeConcentrationVar = dataFile.addVariable(null, "errorWaterVolumeConcentration", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(errorWaterVolumeConcentrationVar, new Attribute("units", "")); //?????
				dataFile.addVariableAttribute(errorWaterVolumeConcentrationVar, new Attribute("long_name", "Water volume concentration error at each time step."));

				if (outVariablesList.contains("SoluteADE") || outVariablesList.contains("all")) {
					
				soluteSourcesSinksTermVar  = dataFile.addVariable(null, "soluteSourcesSinksTerm", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(soluteSourcesSinksTermVar, new Attribute("units", ""));
				dataFile.addVariableAttribute(soluteSourcesSinksTermVar, new Attribute("long_name", "soluteSourcesSinksTerm"));
				
				sumSoluteSourceSinksTermVar = dataFile.addVariable(null, "sumSoluteSourcesSinksTerm", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(sumSoluteSourceSinksTermVar, new Attribute("units", ""));
				dataFile.addVariableAttribute(sumSoluteSourceSinksTermVar, new Attribute("long_name", "sumSoluteSourceSinksTerm."));
				
				waterVolumeConcentrationsVar  = dataFile.addVariable(null, "waterVolumeConcentrations", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(waterVolumeConcentrationsVar, new Attribute("units", "ML-2"));
				dataFile.addVariableAttribute(waterVolumeConcentrationsVar, new Attribute("long_name", "waterVolumeSolute concentration in each control volume."));
				
				averageSoluteConcentrationVar = dataFile.addVariable(null, "averageSoluteConcentration", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(averageSoluteConcentrationVar, new Attribute("units", "ML-3"));
				dataFile.addVariableAttribute(averageSoluteConcentrationVar, new Attribute("long_name", "Average solute concentration."));
				
				averageWaterVolumeSoluteConcentrationVar = dataFile.addVariable(null, "averageWaterVolumeSoluteConcentration", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(averageWaterVolumeSoluteConcentrationVar, new Attribute("units", "ML-2"));
				dataFile.addVariableAttribute(averageWaterVolumeSoluteConcentrationVar, new Attribute("long_name", "Average water volume solute concentration."));
				
				timeVariationsWConcentrationVar = dataFile.addVariable(null, "timeVariationsWConcentration", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(timeVariationsWConcentrationVar, new Attribute("units", "")); //?????
				dataFile.addVariableAttribute(timeVariationsWConcentrationVar, new Attribute("long_name", "Time Variation Water Content Concentration."));
				
				dispersionSoluteFluxesVar = dataFile.addVariable(null, "dispersionSoluteFluxes", DataType.DOUBLE, dualDims);
				dataFile.addVariableAttribute(dispersionSoluteFluxesVar, new Attribute("units", ""));  //?????
				dataFile.addVariableAttribute(dispersionSoluteFluxesVar, new Attribute("long_name", "Dispersion Flux."));
				
				advectionSoluteFluxesVar = dataFile.addVariable(null, "advectionSoluteFluxes", DataType.DOUBLE, dualDims);
				dataFile.addVariableAttribute(advectionSoluteFluxesVar, new Attribute("units", ""));  //?????
				dataFile.addVariableAttribute(advectionSoluteFluxesVar, new Attribute("long_name", "Advection Flux."));
				
				}
				
				
				stressWatersVar  = dataFile.addVariable(null, "StressWaters", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(stressWatersVar, new Attribute("units", "-"));
				dataFile.addVariableAttribute(stressWatersVar, new Attribute("long_name", "Water stress in each control volume"));
				
				stressWaterVar  = dataFile.addVariable(null, "StressWater", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(stressWaterVar, new Attribute("units", "-"));
				dataFile.addVariableAttribute(stressWaterVar, new Attribute("long_name", "water stress"));
				
				evaporationStressWaterVar  = dataFile.addVariable(null, "EvaporationStressWater", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(evaporationStressWaterVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(evaporationStressWaterVar, new Attribute("long_name", "evaporation Stress Water"));
				
				stressSunVar  = dataFile.addVariable(null, "StressSun", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(stressSunVar, new Attribute("units", "-"));
				dataFile.addVariableAttribute(stressSunVar, new Attribute("long_name", "total stressSun"));
				
				stressShadeVar  = dataFile.addVariable(null, "StressShade", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(stressShadeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(stressShadeVar, new Attribute("long_name", "total stressShade"));
				
				stressedETsVar  = dataFile.addVariable(null, "StressedETs", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(stressedETsVar, new Attribute("units", "mm"));
				dataFile.addVariableAttribute(stressedETsVar, new Attribute("long_name", "EvapoTranspired stressed water from BrokerGEO"));
				
				stressedTsVar  = dataFile.addVariable(null, "StressedTs", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(stressedTsVar, new Attribute("units", "mm"));
				dataFile.addVariableAttribute(stressedTsVar, new Attribute("long_name", "Transpired stressed water from BrokerGEO"));
				
				stressedEsVar  = dataFile.addVariable(null, "StressedEs", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(stressedEsVar, new Attribute("units", "mm"));
				dataFile.addVariableAttribute(stressedEsVar, new Attribute("long_name", "Evaporated stressed water from BrokerGEO"));
				
				rootDensityVar  = dataFile.addVariable(null, "RootDensity", DataType.DOUBLE, dims);
				dataFile.addVariableAttribute(rootDensityVar, new Attribute("units", "-"));
				dataFile.addVariableAttribute(rootDensityVar, new Attribute("long_name", "Root density in each control volume"));
				
				errorVar = dataFile.addVariable(null, "errorWaterVolume", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(errorVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(errorVar, new Attribute("long_name", "Volume error at each time step."));
								
				runOffVar = dataFile.addVariable(null, "runOff", DataType.DOUBLE, "time");
				dataFile.addVariableAttribute(runOffVar, new Attribute("units", "m/s"));
				dataFile.addVariableAttribute(runOffVar, new Attribute("long_name", "run off"));

				controlVolumeVar = dataFile.addVariable(null, "controlVolume", DataType.DOUBLE, "depth");
				dataFile.addVariableAttribute(controlVolumeVar, new Attribute("units", "m"));
				dataFile.addVariableAttribute(controlVolumeVar, new Attribute("long_name", "dimension of each control volumes"));


				depth = new ArrayDouble.D1(kDim.getLength());
				dualDepth = new ArrayDouble.D1(dualKDim.getLength());
				dataControlVolume = new ArrayDouble.D1(kDim.getLength());
				dataPsiIC = new ArrayDouble.D1(kDim.getLength());
				dataConcentrationIC = new ArrayDouble.D1(kDim.getLength());
				dataTemperatureIC = new ArrayDouble.D1(kDim.getLength());
				dataRootDensityIC = new ArrayDouble.D1(kDim.getLength());  // queste sono quelle che non cambiano nel tempo

				for (int k = 0; k < kDim.getLength(); k++) {
					depth.set(k, spatialCoordinate[k]);
					dataControlVolume.set(k, controlVolume[k]);
					dataPsiIC.set(k, psiIC[k]);
					dataConcentrationIC.set(k, concentrationIC[k]);	
					dataTemperatureIC.set(k, temperature[k]);
					dataRootDensityIC.set(k, rootIC[k]);
				}
				

				for (int k = 0; k < dualKDim.getLength(); k++) {
					dualDepth.set(k, dualSpatialCoordinate[k]);
				}

				//Create the file. At this point the (empty) file will be written to disk
				dataFile.create();
				dataFile.write(depthVar, depth);
				dataFile.write(dualDepthVar, dualDepth);
				dataFile.write(controlVolumeVar, dataControlVolume);
				dataFile.write(psiICVar, dataPsiIC);
				dataFile.write(concentrationICVar, dataConcentrationIC);
				dataFile.write(rootDensityICVar, dataRootDensityIC);
				dataFile.write(temperatureICVar, dataTemperatureIC);
				stepCreation = 1;

				System.out.println("\n\t***Created NetCDF " + fileNameToSave +"\n\n");
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (dataFile != null)
					try {
						dataFile.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
			}

		}


		/*
		 * Write data
		 */

		if( step%writeFrequency==0 || doProcess == false) {


			try {

				dataFile = NetcdfFileWriter.openExisting(fileNameToSave);

				// number of time record that will be saved
				NREC = variables.keySet().size();

				times = Array.factory(DataType.INT, new int[] {NREC});

				dataPsi = new ArrayDouble.D2(NREC, KMAX); // aggiungere le mie variabili a matrici 
				dataTheta = new ArrayDouble.D2(NREC, KMAX);
				dataETs = new ArrayDouble.D2(NREC, KMAX);
				dataWaterVolume = new ArrayDouble.D2(NREC, KMAX);
				dataError = new ArrayDouble.D1(NREC); // aggiungere le mie variabili a vettori 
				
				dataRunOff = new ArrayDouble.D1(NREC);
				
				dataStressWaters = new ArrayDouble.D2(NREC, KMAX); 
				dataStressedETs = new ArrayDouble.D2(NREC, KMAX); 
				
				dataStressWater = new ArrayDouble.D1(NREC);
				dataEvaporationStressWater = new ArrayDouble.D1(NREC);
				dataStressSun = new ArrayDouble.D1(NREC);
				dataStressShade = new ArrayDouble.D1(NREC);
				
				dataStressedTs = new ArrayDouble.D2(NREC, KMAX);
				dataStressedEs = new ArrayDouble.D2(NREC, KMAX);
				dataRootDensity = new ArrayDouble.D2(NREC, KMAX);
				
				dataConcentrations 		= new ArrayDouble.D2(NREC, KMAX);
				dataErrorWaterVolumeConcentration = new ArrayDouble.D1(NREC);
				
				if (outVariablesList.contains("SoluteADE") || outVariablesList.contains("all")) {
					dataWaterVolumeConcentrations 	= new ArrayDouble.D2(NREC, KMAX);
					dataTimeVariationsWConcentration= new ArrayDouble.D2(NREC, KMAX);
					dataDispersionSoluteFluxes 		= new ArrayDouble.D2(NREC, DUALKMAX);
					dataAdvectionSoluteFluxes 		= new ArrayDouble.D2(NREC, DUALKMAX);
					dataSoluteSourceSinksTerm		= new ArrayDouble.D2(NREC, KMAX);
					dataAverageSoluteConcentration = new ArrayDouble.D1(NREC);
					dataAverageWaterVolumeSoluteConcentration = new ArrayDouble.D1(NREC);
					dataSumSoluteSourceSinksTerm = new ArrayDouble.D1(NREC);
				}
				



				if (outVariablesList.contains("darcyVelocity") || outVariablesList.contains("all")) {
					dataDarcyVelocities = new ArrayDouble.D2(NREC, KMAX);
				}
				
				if (outVariablesList.contains("darcyVelocityCapillary") || outVariablesList.contains("all")) {
					dataDarcyVelocitiesCapillary = new ArrayDouble.D2(NREC, KMAX);
				}
				
				if (outVariablesList.contains("darcyVelocityGravity") || outVariablesList.contains("all")) {
					dataDarcyVelocitiesGravity = new ArrayDouble.D2(NREC, KMAX);
				}
				
				if (outVariablesList.contains("poreVelocity") || outVariablesList.contains("all")) {
					dataPoreVelocities = new ArrayDouble.D2(NREC, KMAX);
				}
				
				if (outVariablesList.contains("celerity") || outVariablesList.contains("all")) {
					dataCelerity = new ArrayDouble.D2(NREC, KMAX);
				}
				
				if (outVariablesList.contains("kinematicRatio") || outVariablesList.contains("all")) {
					dataKinematicRatio = new ArrayDouble.D2(NREC, KMAX);
				}	
				
				if (outVariablesList.contains("boundaryConditions") || outVariablesList.contains("all")) {
					dataTopBC = new ArrayDouble.D1(NREC);
					dataBottomBC = new ArrayDouble.D1(NREC);
				}
				
////////////////////////////////////
				
				int i=0;
				it = variables.entrySet().iterator();
				while (it.hasNext()) {

					@SuppressWarnings("unchecked")
					Entry<String, ArrayList<double[]>> entry = (Entry<String, ArrayList<double[]>>) it.next();

					try {
						date = dateFormat.parse(entry.getKey());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					times.setLong(i, (long) date.getTime()/(60*1000));


					tempVariable =  entry.getValue().get(0);
					for (int k = 0; k < KMAX; k++) {
						dataPsi.set(i, k, tempVariable[k]);}


					tempVariable =  entry.getValue().get(1);
					for (int k = 0; k < KMAX; k++) {
						dataTheta.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(2);
					for (int k = 0; k < KMAX; k++) {
						dataWaterVolume.set(i, k, tempVariable[k]);}
					
					
					
					
					if (outVariablesList.contains("darcyVelocity") || outVariablesList.contains("all")) {
						tempVariable =  entry.getValue().get(3);
						for (int k = 0; k < DUALKMAX; k++) {
							dataDarcyVelocities.set(i, k, tempVariable[k]);}}
					
					if (outVariablesList.contains("darcyVelocityCapillary") || outVariablesList.contains("all")) {
						tempVariable =  entry.getValue().get(4);
						for (int k = 0; k < DUALKMAX; k++) {
							dataDarcyVelocitiesCapillary.set(i, k, tempVariable[k]);}}

					if (outVariablesList.contains("darcyVelocityGravity") || outVariablesList.contains("all")) {
						tempVariable =  entry.getValue().get(5);
						for (int k = 0; k < DUALKMAX; k++) {
							dataDarcyVelocitiesGravity.set(i, k, tempVariable[k]);}}

					if (outVariablesList.contains("poreVelocity") || outVariablesList.contains("all")) {
						tempVariable =  entry.getValue().get(6);
						for (int k = 0; k < DUALKMAX; k++) {
							dataPoreVelocities.set(i,k, tempVariable[k]);}}

					if (outVariablesList.contains("celerity") || outVariablesList.contains("all")) {
						tempVariable =  entry.getValue().get(7);
						for (int k = 0; k < DUALKMAX; k++) {
							dataCelerity.set(i,k, tempVariable[k]);}}
					
					if (outVariablesList.contains("kinematicRatio") || outVariablesList.contains("all")) {
						tempVariable =  entry.getValue().get(8);
						for (int k = 0; k < DUALKMAX; k++) {
							dataKinematicRatio.set(i,k, tempVariable[k]);}}

					if (outVariablesList.contains("boundaryConditions") || outVariablesList.contains("all")) {
						dataTopBC.set(i, entry.getValue().get(11)[0]);
						dataBottomBC.set(i, entry.getValue().get(12)[0]);}
					
					
					
					tempVariable =  entry.getValue().get(9);
					for (int k = 0; k < KMAX; k++) {
						dataETs.set(i,k, tempVariable[k]);}

					dataError.set(i, entry.getValue().get(10)[0]);

					dataRunOff.set(i, entry.getValue().get(13)[0]);
					
					
					
					tempVariable =  entry.getValue().get(14);
					for (int k = 0; k < KMAX; k++) {
						dataConcentrations.set(i, k, tempVariable[k]);}
					
					dataErrorWaterVolumeConcentration.set(i, entry.getValue().get(21)[0]);
					

					if (outVariablesList.contains("SoluteADE") || outVariablesList.contains("all")) {
					tempVariable =  entry.getValue().get(15);
					for (int k = 0; k < KMAX; k++) {
						dataWaterVolumeConcentrations.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(16);
					for (int k = 0; k < KMAX; k++) {
						dataSoluteSourceSinksTerm.set(i, k, tempVariable[k]);}
					
					dataSumSoluteSourceSinksTerm.set(i, entry.getValue().get(17)[0]);
					
					tempVariable =  entry.getValue().get(18);
					for (int k = 0; k < DUALKMAX; k++) {
						dataTimeVariationsWConcentration.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(19);
					for (int k = 0; k < DUALKMAX; k++) {
						dataDispersionSoluteFluxes.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(20);
					for (int k = 0; k < DUALKMAX; k++) {
						dataAdvectionSoluteFluxes.set(i, k, tempVariable[k]);}
					
					dataAverageSoluteConcentration.set(i, entry.getValue().get(22)[0]);
					
					dataAverageWaterVolumeSoluteConcentration.set(i, entry.getValue().get(23)[0]);

					}
					
					
					tempVariable =  entry.getValue().get(24);
					for (int k = 0; k < KMAX-1; k++) {
						dataStressWaters.set(i, k, tempVariable[k]);}
					
					dataStressWater.set(i, entry.getValue().get(25)[0]);
					
					dataEvaporationStressWater.set(i, entry.getValue().get(26)[0]);
					
					dataStressSun.set(i, entry.getValue().get(27)[0]);
					
					dataStressShade.set(i, entry.getValue().get(28)[0]);
					
					tempVariable =  entry.getValue().get(29);
					for (int k = 0; k < KMAX-1; k++) {
						dataStressedETs.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(30);
					for (int k = 0; k < KMAX-1; k++) {
						dataRootDensity.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(31);
					for (int k = 0; k < KMAX-1; k++) {
						dataStressedTs.set(i, k, tempVariable[k]);}
					
					tempVariable =  entry.getValue().get(32);
					for (int k = 0; k < KMAX-1; k++) {
						dataStressedEs.set(i, k, tempVariable[k]);}

					i++;
				}				

				// A newly created Java integer array to be initialized to zeros.
//				origin[0] = dataFile.findVariable("psi").getShape()[0];
//				dual_origin[0] = dataFile.findVariable("darcy_velocity").getShape()[0];
//				time_origin[0] = dataFile.findVariable("time").getShape()[0];

				origin[0] = origin_counter;
				time_origin[0] = origin_counter;
				
				//				dataFile.write(kIndexVar, kIndex);
				dataFile.write(dataFile.findVariable("time"), time_origin, times);
				dataFile.write(dataFile.findVariable("psi"), origin, dataPsi);
				dataFile.write(dataFile.findVariable("theta"), origin, dataTheta);
				dataFile.write(dataFile.findVariable("waterVolume"), origin, dataWaterVolume);
				
				if (outVariablesList.contains("darcyVelocity") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("darcyVelocity"), origin, dataDarcyVelocities);}
				
				if (outVariablesList.contains("darcyVelocityCapillary") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("darcyVelocityCapillary"), origin, dataDarcyVelocitiesCapillary);}
				
				if (outVariablesList.contains("darcyVelocityGravity") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("darcyVelocityGravity"), origin, dataDarcyVelocitiesGravity);}
				
				if (outVariablesList.contains("poreVelocity") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("poreVelocity"), origin, dataPoreVelocities);}
				
				if (outVariablesList.contains("celerity") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("celerity"), origin, dataCelerity);}
				
				if (outVariablesList.contains("kinematicRatio") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("kinematicRatio"), origin, dataKinematicRatio);}
				
				if (outVariablesList.contains("boundaryConditions") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("topBC"), time_origin, dataTopBC);
					dataFile.write(dataFile.findVariable("bottomBC"), time_origin, dataBottomBC);}
				
				if (outVariablesList.contains("SoluteADE") || outVariablesList.contains("all")) {
					dataFile.write(dataFile.findVariable("soluteSourcesSinksTerm"), origin, dataSoluteSourceSinksTerm);
					dataFile.write(dataFile.findVariable("sumSoluteSourcesSinksTerm"), time_origin, dataSumSoluteSourceSinksTerm);
					dataFile.write(dataFile.findVariable("waterVolumeConcentrations"), origin, dataWaterVolumeConcentrations);
					dataFile.write(dataFile.findVariable("timeVariationsWConcentration"), origin, dataTimeVariationsWConcentration);
					dataFile.write(dataFile.findVariable("dispersionSoluteFluxes"), origin, dataDispersionSoluteFluxes);
					dataFile.write(dataFile.findVariable("advectionSoluteFluxes"), origin, dataAdvectionSoluteFluxes);
					dataFile.write(dataFile.findVariable("averageSoluteConcentration"), time_origin, dataAverageSoluteConcentration);
					dataFile.write(dataFile.findVariable("averageWaterVolumeSoluteConcentration"), time_origin, dataAverageWaterVolumeSoluteConcentration);}
				
				dataFile.write(dataFile.findVariable("errorWaterVolumeConcentration"), time_origin, dataErrorWaterVolumeConcentration);
				dataFile.write(dataFile.findVariable("concentrations"), origin, dataConcentrations);
				
				dataFile.write(dataFile.findVariable("ets"), origin, dataETs);
				
				dataFile.write(dataFile.findVariable("StressWaters"), origin, dataStressWaters);
				dataFile.write(dataFile.findVariable("StressedETs"), origin, dataStressedETs);
				
				dataFile.write(dataFile.findVariable("StressWater"), time_origin, dataStressWater);
				dataFile.write(dataFile.findVariable("EvaporationStressWater"), time_origin, dataEvaporationStressWater);
				dataFile.write(dataFile.findVariable("StressSun"), time_origin, dataStressSun);
				dataFile.write(dataFile.findVariable("StressShade"), time_origin, dataStressShade);
				
				dataFile.write(dataFile.findVariable("StressedTs"), origin, dataStressedTs);
				dataFile.write(dataFile.findVariable("StressedEs"), origin, dataStressedEs);
				dataFile.write(dataFile.findVariable("RootDensity"), origin, dataRootDensity);

				dataFile.write(dataFile.findVariable("errorWaterVolume"), time_origin, dataError);
				dataFile.write(dataFile.findVariable("runOff"), time_origin, dataRunOff);

				origin_counter = origin_counter + NREC;
				
				
				fileSizeMB = 1*KMAX*8*origin_counter/1000000;
//				System.out.println("\t\tfileSizeMB: " + fileSizeMB);
				stepCreation ++;
				if(fileSizeMB>fileSizeMax) {
					stepCreation = 0;
					fileNumber++;
					NREC = 0;
				}
					
				if(!variables.isEmpty()) {
					System.out.println("\n\n\t\t*** " + variables.keySet().toArray()[i-1].toString() +", writing output file: " + fileNameToSave + "\n");
				}


			} catch (IOException e) {
				e.printStackTrace(System.err);

			} catch (InvalidRangeException e) {
				e.printStackTrace(System.err);

			} finally {
				if (dataFile != null)
					try {
						dataFile.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
			}

		}

		step++;
	}


}


