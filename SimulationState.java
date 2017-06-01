
import java.text.DecimalFormat;
import java.util.*;

public class SimulationState {
	private Map<String,Integer> population;
	private Map<String,Double> percentages;
	
	private Map<String,Double> averageHappiness = new TreeMap<String,Double>();
	
	private final double ERROR = 0.01;

	public SimulationState(List<Human> list, List<Human> active) {
		Map<String,Integer> types = new TreeMap<String,Integer>();
		Map<String,Double> perc = new TreeMap<String,Double>();
		List<Human> snapshot = new LinkedList<Human>(list);
		//System.out.println(snapshot);
		
		for(Human h : snapshot) {
			//System.out.println(h);
			if(h==null) continue;
			if(types.containsKey(h.getType())) {
				types.put(h.getType(), types.get(h.getType())+1);
			} else {
				types.put(h.getType(), 1);
			}
			
		}
		
		Map<String,Integer> activeTypes = new TreeMap<String,Integer>();
		snapshot = new LinkedList<Human>(active);
		for(Human h : snapshot) {
			//System.out.println(h);
			if(h==null) continue;
			if(activeTypes.containsKey(h.getType())) {
				activeTypes.put(h.getType(), activeTypes.get(h.getType())+1);
			} else {
				activeTypes.put(h.getType(), 1);
			}
			
		}
		
		for(Human h : snapshot) {
			if(h==null) continue;
			if(averageHappiness.containsKey(h.getType())) {
				averageHappiness.put(h.getType(),averageHappiness.get(h.getType())+h.getHappiness());
			} else {
				averageHappiness.put(h.getType(), h.getHappiness());
			}
		}
		
		this.population = types;
		for(String k : types.keySet()) {
			perc.put(k, (types.get(k)*100d/getPopulationNumber()));
			if(activeTypes.containsKey(k))
				averageHappiness.put(k, averageHappiness.get(k)/activeTypes.get(k));
		}
		this.percentages = perc;
		//System.out.println(types);
	}

	public Map<String, Integer> getPopulation() {
		return population;
	}

	public void setPopulation(Map<String, Integer> population) {
		this.population = population;
	}

	public Map<String, Double> getPercentages() {
		return percentages;
	}

	public void setPercentages(Map<String, Double> percentages) {
		this.percentages = percentages;
	}

	@Override
	public String toString() {
		String s = "";
		Map<String,Integer> state = this.population;
		Map<String,Double> perc = this.percentages;
		int humanNumber = this.getPopulationNumber();
		DecimalFormat df = new DecimalFormat("#.##");
		
		final int MAX_COLS = 80;
		final int MAX_LINES = 10;
		int num_types = perc.keySet().size();
		
		for(int i=0;i<MAX_LINES;i++) {
			
			for(String k : perc.keySet()) {
				String spaces = new String(new char[MAX_COLS/num_types]).replace('\0', ' ');
				s += spaces;
				if(perc.get(k)>(MAX_LINES-i)*100/MAX_LINES)
					s += "|";
				else {
					s += " ";
				}
				
			}
			s += "\n";
		}

		int prev = 0;
		for(String k : perc.keySet()) {
			String info = k+": "+df.format(perc.get(k))+"% "+state.get(k)+" ";
			int current_spaces = info.length()/2;
			String spaces = new String(new char[(MAX_COLS/num_types)-(current_spaces+prev)]).replace('\0', ' ');
			s += spaces+info;
			prev = current_spaces;
		}
		prev = 0;
		s += "\n";
		for(Population.SubPopulation t : Simulator.getPopulation().threadPools) {
			String info = "Threads "+t.getName()+": "+t.getActiveCount();
			int current_spaces = info.length()/2;
			String spaces = new String(new char[(MAX_COLS/num_types)-(current_spaces+prev)]).replace('\0', ' ');
			s += spaces+info;
			prev = current_spaces;
		}
		s += "\npopulation: "+humanNumber;
		s += "\nThreads: "+Simulator.getPopulation().getTotalThreads()+"\n";
		s += "Active Humans: "+Simulator.getPopulation().alive.size()+"\n";
		Map<String,Double> data = this.getObservingData();
		for(String k : data.keySet()) {
			s += k;
			s += ": "+df.format(data.get(k));
			s += " ";
		}
		s += "  Avg happiness: ";
		for(String k : averageHappiness.keySet()) {
			s += k;
			s += ": "+df.format(averageHappiness.get(k));
			s += " ";
		}
		s += " Male: "+df.format(Simulator.getPopulation().getThresholdForGender(Gender.MALE,this));
		s += " Female: "+df.format(Simulator.getPopulation().getThresholdForGender(Gender.FEMALE,this));
		return s;
	}

	private Map<String,Double> getObservingData() {
		Map<String,Double> data = new HashMap<String,Double>();
		for(String k : Simulator.getPopulation().getObservingData().keySet()) {
			String v = Simulator.getPopulation().getObservingData().get(k);
			String key = k+"/"+v+"+"+k;
			if(population.containsKey(k) && population.containsKey(v)) {
				double value = ((double)this.population.get(k)/(this.population.get(v)+this.population.get(k)));
				data.put(key, value);
			}
			
			
		}
		return data;
	}

	private int getPopulationNumber() {
		int n = 0;
		for(String s : population.keySet()) {
			n += population.get(s);
		}
		return n;
	}

	public boolean isNear(SimulationState otherState) {
		Map<String,Double> data,otherData;
		data = this.getPercentages();
		otherData = otherState.getPercentages();
		for(String k : data.keySet()) {
			if(otherData.containsKey(k)) {
				double perc1, perc2;
				perc1 = data.get(k);
				perc2 = otherData.get(k);
				if(!almostEqual(perc1,perc2, ERROR)) {
					//System.out.println(""+data.get(k)+" "+otherData.get(k));
					return false;
				}
			} else {
				//System.out.println(""+data.get(k)+" "+otherData.get(k));
				return false;
			}
		}
		return true;

	}

	private boolean almostEqual(double a, double b, double eps){
		//System.out.println(""+a+" + "+b+" = "+Math.abs(a-b)+"<"+eps+" : "+(Math.abs(a-b)<eps));
		return Math.abs(a-b)<eps;
	}

	public double getAverageHappiness(String type) {
		if(averageHappiness.containsKey(type))
			return averageHappiness.get(type);
		else
			return 0;
	}

}