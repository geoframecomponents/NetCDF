/*
 * GNU GPL v3 License
 *
 * Copyright 2016 Marialaura Bancheri
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

package monodimensionalProblemTimeDependent;

import java.io.IOException;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;




@Description("This class reads a NetCDF containing 1D grid data. The input file is created with a Jupyter Notebook")
@Documentation("")
@Author(name = "Niccolo' Tubini, Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, Infiltration")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")


public class ReadNetCDFRichardsGrid1D {

	@Description("File name of NetCDF containing grid data")
	@In
	public String richardsGridFilename;


	@Description("eta coordinate of volume centroids: zero is at soil surface and and positive upward")
	@Out
	public double[] eta;

	@Description("eta coordinate of volume interfaces: zero is at soil surface and and positive upward.")
	@Out
	public double[] etaDual;

	@Description("z coordinate  of volume centroids: zero is at the bottom of the column and and positive upward")
	@Out
	public double[] z;

	@Description("z coordinate of volume interfaces: zero is at soil surface and and positive upward.")
	@Out
	public double[] zDual;

	@Description("Initial condition for water suction")
	@Out
	public double[] psiIC;
	
	@Description("Distance between consecutive controids, is used to compute gradients")
	@Out
	public double[] spaceDelta;
	
	@Description("Length of each control volume")
	@Out
	public double[] deltaZ;
	
	@Description("Adimensional water content at saturation")
	@Out
	public double[] thetaS;
	
	@Description("Adimensional residual water content")
	@Out
	public double[] thetaR;
	
	@Description("Hydraulic conductivity at saturation")
	@Out
	public double[] Ks;


	int[] size;

	@Execute
	public void read() throws IOException{

		// Open the file. The ReadOnly parameter tells netCDF we want
		// read-only access to the file.
		NetcdfFile dataFile = null;
		String filename = richardsGridFilename;
		// Open the file.
		try {

			dataFile = NetcdfFile.open(filename, null);

			// Retrieve the variables named "___"
			Variable dataEta = dataFile.findVariable("eta");
			Variable dataEtaDual = dataFile.findVariable("etaDual");
			Variable dataZ = dataFile.findVariable("z");
			Variable dataZDual = dataFile.findVariable("zDual");
			Variable dataPsiIC = dataFile.findVariable("psiIC");
			Variable dataSpaceDelta = dataFile.findVariable("spaceDelta");
			Variable dataDeltaZ = dataFile.findVariable("deltaZ");
			Variable dataThetaS = dataFile.findVariable("thetaS");
			Variable dataThetaR = dataFile.findVariable("thetaR");
			Variable dataKs = dataFile.findVariable("Ks");



			//if (dataEta == null) {
			//	System.out.println("Cant find Variable data");
			//	return;
			//}


			size = dataEta.getShape();

			eta     = new double[size[0]];
			etaDual = new double[size[0]];
			z       = new double[size[0]];
			zDual   = new double[size[0]];
			psiIC   = new double[size[0]];
			spaceDelta = new double[size[0]];

			ArrayDouble.D1 dataArrayEta;
			ArrayDouble.D1 dataArrayEtaDual;
			ArrayDouble.D1 dataArrayZ;
			ArrayDouble.D1 dataArrayZDual;
			ArrayDouble.D1 dataArrayPsiIC;
			ArrayDouble.D1 dataArraySpaceDelta;

			dataArrayEta     = (ArrayDouble.D1) dataEta.read(null,size);
			dataArrayEtaDual = (ArrayDouble.D1) dataEtaDual.read(null,size);
			dataArrayZ       = (ArrayDouble.D1) dataZ.read(null,size);
			dataArrayZDual   = (ArrayDouble.D1) dataZDual.read(null,size);
			dataArrayPsiIC   = (ArrayDouble.D1) dataPsiIC.read(null,size);
			dataArraySpaceDelta = (ArrayDouble.D1) dataSpaceDelta.read(null,size);

			for (int i = 0; i < size[0]; i++) {


				eta[i]     = dataArrayEta.get(i);
				etaDual[i] = dataArrayEtaDual.get(i);
				z[i]       = dataArrayZ.get(i);
				zDual[i]   = dataArrayZDual.get(i);
				psiIC[i]   = dataArrayPsiIC.get(i);
				spaceDelta[i] = dataArraySpaceDelta.get(i);


			}

			//////////////////////////
			//////////////////////////
			//////////////////////////

			size = dataDeltaZ.getShape();

			deltaZ     = new double[size[0]];
			thetaS     = new double[size[0]];
			thetaR     = new double[size[0]];
			Ks         = new double[size[0]];

			ArrayDouble.D1 dataArrayDeltaZ;
			ArrayDouble.D1 dataArrayThetaS;
			ArrayDouble.D1 dataArrayThetaR;
			ArrayDouble.D1 dataArrayKs;


			dataArrayDeltaZ     = (ArrayDouble.D1) dataDeltaZ.read(null,size);
			dataArrayThetaS     = (ArrayDouble.D1) dataThetaS.read(null,size);
			dataArrayThetaR     = (ArrayDouble.D1) dataThetaR.read(null,size);
			dataArrayKs         = (ArrayDouble.D1) dataKs.read(null,size);


			for (int i = 0; i < size[0]; i++) {

				System.out.println(dataArrayDeltaZ.get(i));
				deltaZ[i]     = dataArrayDeltaZ.get(i);
				thetaS[i]     = dataArrayThetaS.get(i);
				thetaR[i]     = dataArrayThetaR.get(i);
				Ks[i]         = dataArrayKs.get(i);


			}
			/*
			System.out.println("Check IC values:\n\n");
			for (int i = 0; i < size[0]; i++) {

				System.out.println("	"+psiIC[i]);

			}
			 */

		} catch (InvalidRangeException e) {
			e.printStackTrace();

		} finally {
			if (dataFile != null)
				try {
					dataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}

		System.out.println("*** SUCCESS reading example file " + richardsGridFilename);


	}
}