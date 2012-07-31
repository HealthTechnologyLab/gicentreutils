package org.gicentre.utils.network.traer.physics;

import java.util.*;

// *****************************************************************************************
/** Represents an entire particle system containing particles and forces between them.
 *  @author Jeffrey Traer Bernstein, Carl Pearson and minor modifications by Jo Wood.
 *  @since 4.0
 */
// *****************************************************************************************

/* This file is included in the giCentre utilities library, but was developed independently by
 * Jeff Traer and Carl Pearson. See http://murderandcreate.com/physics and
 * http://code.google.com/p/traerphysics/. The traer.physics package is distributed under the
 * Artistic Licence: http://dev.perl.org/licenses/
 */

public class ParticleSystem 
{

	// --------------------------- Class and object variables -----------------------------

	/** Indicates a Runge Kutta integrator.
	 *  @deprecated Use the enum values defined in the {@link Integrator} class in preference to this.
	 */
	public static final int RUNGE_KUTTA = Integrator.METHOD.RUNGEKUTTA.ordinal();
	
	/** Indicates a modified Euler integrator.
	 *  @deprecated Use the enum values defined in the {@link Integrator} class in preference to this.
	 */
	public static final int MODIFIED_EULER = Integrator.METHOD.MODEULER.ordinal();

	
				/** Default drag on all particles. */
	protected static final float DEFAULT_DRAG = 0.001f;  
				/** The default magnitude for the y-component of gravity. */
	protected static final float DEFAULT_GRAVITY = 0;

	private List<Particle> particles = new ArrayList<Particle>();
	private List<Spring> springs = new ArrayList<Spring>();
	private List<Attraction> attractions = new ArrayList<Attraction>();
	private List<AbstractForce> customForces = new ArrayList<AbstractForce>();
	//private Map<String,UniversalForce> uForces = new HashMap<String,UniversalForce>();
	
	private float deltaT = 1f; 			// The time step to use with {@link #tick()}; set to 1 by default.
	private Integrator integrator;		// The integrator that modifies particles on each time step.
	private Vector3D gravity;			// The gravity vector for this ParticleSystem.
	private float drag;					// The drag magnitude for this ParticleSystem.
	
	// ---------------------------------- Constructors ------------------------------------
	
	/** Creates a particle system with no gravity and default drag on particles. By default the 
	 *  system will use Runge Kutta integration to modify particles. Can be set to Euler or
	 *  other integrators with <code>setIntegrator()</code>.
	 */
	public ParticleSystem() 
	{
		this(ParticleSystem.DEFAULT_GRAVITY,ParticleSystem.DEFAULT_DRAG);
	}
	
	/** Creates a particle system with the given gravitational pull and drag. By default the 
	 *  system will use Runge Kutta integration to modify particles. Can be set to Euler or
	 *  other integrators with <code>setIntegrator()</code>.
	 *  @param g Gravitational parameter, applied in the 'y' direction only.
	 *  @param drag The drag associated with particles in this system.
	 */
	public ParticleSystem(float g, float drag)
	{ 
		this(0,g,0,drag); 
	}

	/** Creates a particle system with the given 3d gravitational pull and drag. By default the 
	 *  system will use Runge Kutta integration to modify particles. Can be set to Euler or
	 *  other integrators with <code>setIntegrator()</code>.
	 *  @param gx Gravitational parameter, applied in the 'x' direction.
	 *  @param gy Gravitational parameter, applied in the 'y' direction.
	 *  @param gz Gravitational parameter, applied in the 'z' direction.
	 *  @param drag The drag associated with particles in this system.
	 */
	public ParticleSystem(float gx, float gy, float gz, float drag) 
	{
		setIntegrator(Integrator.METHOD.RUNGEKUTTA);
		gravity = new Vector3D(gx, gy, gz);
		setDrag(drag);
	}
	
	// ------------------------------------ Methods --------------------------------------- 

	/** Sets the size of the time step used with {@link Integrator#step(float)} for this ParticleSystem.
	 * @param t the time step size
	 * @return this ParticleSystem with its new time step size.
	 * @throws IllegalArgumentException if t<=0
	 */
	public final ParticleSystem	setDeltaT(float t)	throws IllegalArgumentException 
	{
		if (t<=0)
		{
			throw new IllegalArgumentException("Argument t is "+t+"; t must be >=0.");
		}
		
		deltaT = t; 
		return this; 
	}
	
	/** Reports the current time step size used by this ParticleSystem's Integrator.
	 *  @return the time step size
	 */
	public final float getDeltaT()
	{
		return deltaT; 
	}
	
	/** Advances this ParticleSystem's Integrator by the local time step.  Uses {@link #tick(float)}.
	 *  @return this ParticleSystem, post the advance.
	 */
	public final ParticleSystem tick() 
	{ 
		return tick(deltaT); 
	}
	
	/** Advances this ParticleSystem's Integrator by a user-specified time step.
	 *  @param t the amount of time to advance
	 *  @return this ParticleSystem, post the advance.
	 *  @throws IllegalArgumentException if t<=0
	 */
	public final ParticleSystem tick( float t ) 
	{
		if (t<=0) 
		{
			throw new IllegalArgumentException("Argument t is "+t+"; t must be >=0.");
		}
		integrator.step(t);
		return this;
	}
	
	/** Sets the integrator for this particle system based on the specified integrator ID.
	 *  @deprecated Consider replacing  with {@link #setIntegrator(Integrator.METHOD)}.
	 *  @param integrator the ID corresponding to the desired Integrator
	 *  @return this ParticleSystem, using the specified Integrator
	 *  @throws IllegalArgumentException if the argument does not correspond to defined Integrator
	 */
	@Deprecated public final ParticleSystem setIntegrator(int integrator) throws IllegalArgumentException
	{
		try 
		{
			return setIntegrator(Integrator.METHOD.values()[integrator]);
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			return illegalArgThrower("Argument integrator does not correspond to a valid Integrator; consult Integrator class for valid options.");
		}
	}
	
	/** Sets the integrator for this particle system based on the specified integrator name.
	 *  @param integrator Name of integrator to be used by this particle system.
	 *  @return this ParticleSystem, using the specified integrator.
	 */
	public final ParticleSystem	setIntegrator(Integrator.METHOD integrator)
	{ 
		return setIntegrator(integrator.factory(this)); 
	}
	
	/** Sets the integrator for this particle system based on the specified integrator.
	 *  @param integrator the desired integrator.
	 *  @return this ParticleSystem, using the specified integrator.
	 *  @throws NullPointerException if integrator==null
	 */
	public final ParticleSystem	setIntegrator(Integrator integrator) throws NullPointerException
	{
		nullThrower(integrator, "Argument integrator is null in setIntegrator(integrator) call.");
		this.integrator = integrator;
		return this;
	}

	/** Sets the x, y, z components of the gravity vector.
	 * @param x the x component of the gravity vector.
	 * @param y the y component of the gravity vector.
	 * @param z the z component of the gravity vector.
	 * @return this ParticleSystem with its new gravity.
	 */
	public final ParticleSystem	setGravity(float x, float y, float z)
	{ 
		gravity.set( x, y, z );
		return this;
	}
	
	/** Sets the gravity with 0,g,0 components.
	 *  @param g the y component of the gravity vector.
	 *  @return this ParticleSystem with its new gravity.
	 */
	public final ParticleSystem	setGravity(float g)
	{
		return setGravity(0, g, 0);
	}

	/** Sets the drag component that affects the particles in this system.
	 *  @param d the drag factor. A positive value corresponds to physical drag.
	 *  @return this ParticleSystem with its updated drag value.
	 */
	public final ParticleSystem setDrag(float d) 
	{ 
		drag = d; 
		return this;
	}

	/** Creates a particle in the ParticleSystem, and returns that Particle
	 * @param mass the new Particle mass
	 * @param x the x position
	 * @param y the y position
	 * @param z the z position
	 * @return the new Particle
	 */
	public final Particle makeParticle(float mass, float x, float y, float z) 
	{
		Particle p = new Particle(mass);
		p.position().set(x, y, z);
		particles.add(p);
		return p;
	}

	/** Creates a Particle with {@link Particle#DEFAULT_MASS} and position=(0,0,0).
	 * @return the new Particle
	 */
	public final Particle makeParticle() 
	{ 
		Particle p = new Particle();
		particles.add(p);
		return p;
	}

	/** Adds a custom Particle to the ParticleSystem, and returns the ParticleSystem.
	 * @param p the custom particle
	 * @return p; there are no side-effects to p
	 * @throws NullPointerException if p==null
	 */
	public final Particle makeParticle(Particle p) 
	{
		nullThrower(p, "Argument p is null in makeParticle(p) call.");
		particles.add(p);
		return p;
	}

	/** Creates a spring between the given particles with the given strength, damping and reset length.
	 *  Strength - If they are strong they act like a stick. If they are weak they take a long time to
	 *  return to their rest length. Rest Length - the spring wants to be at this length and acts on the
	 *  particles to push or pull them exactly this far apart at all times. Damping - If springs have high
	 *  damping they don't overshoot and they settle down quickly, with low damping springs oscillate.
	 *  @param a First particle to be joined with the spring.
	 *  @param b Second particle to be joined with the spring.
	 *  @param ks Strength of the spring.
	 *  @param d The damping component of the spring.
	 *  @param r Rest length of the spring.
	 *  @return Spring with the given properties.
	 *  @throws NullPointerException if either of the particles are null.
	 */
	public final Spring	makeSpring(Particle a, Particle b, float ks, float d, float r) throws NullPointerException 
	{
		Spring s = new Spring(a, b, ks, d, r);
		springs.add( s );
		return s;
	}
	
	/** Creates an attractive force between the given particles with the given strength. If the strength is negative the
	 *  particles will repel each other, if the strength is positive they attract. The given  minimum distance limits
	 *  how strong this force can get close up.
	 *  @param a First particle to be associated with the attraction.
	 *  @param b Second particle to be associated with the attraction.
	 *  @param k Strength of the attraction, positive to bring particles together, negative to repulse.
	 *  @param minDistance Minimum distance below which the attraction is not applied.
	 *  @return The new attractive force.
	 *  @throws NullPointerException if either of the particles is null.
	 */
	public final Attraction	makeAttraction(Particle a, Particle b, float k, float minDistance) throws NullPointerException 
	{
		Attraction m = new Attraction(a, b, k, minDistance);
		attractions.add(m);
		return m;
	}
	
	/** Reports a list of the springs currently defined as part of this particle system.
	 *  @return List of springs.
	 *  @deprecated Replace in favour of the more consistently named getSprings().
	 */
	public final List<Spring> springs()
	{ 
		return getSprings(); 
	}
	
	/** Reports a list of the springs currently defined as part of this particle system.
	 *  @return List of springs.
	 */
	public final List<Spring> getSprings()
	{ 
		return springs; 
	}

	/** Reports the number of springs in this particle system.
	 *  @return Number of springs in the system.
	 *  @deprecated Replace in favour of the more consistently named getNumSprings().
	 */
	public final int numberOfSprings() 
	{ 
		return getNumSprings(); 
	}
	
	/** Reports the number of springs in this particle system.
	 *  @return Number of springs in the system.
	 */
	public final int getNumSprings() 
	{ 
		return springs.size(); 
	}
	
	/** Provides the spring at the given position in the list of springs stored in this particle system.
	 *  @param i List index (the ith spring in the list).
	 *  @return The spring requested.
	 */
	public final Spring getSpring(int i) 
	{ 
		return springs.get(i);
	}
	
	/** Removes the spring at the given position in the list of springs stored in this particle system.
	 *  @param i List index (the ith spring in the list).
	 *  @return The spring removed.
	 */
	public final Spring removeSpring(int i)
	{ 
		return springs.remove(i);
	}
	
	/** Removes the given spring from the list of springs stored in this particle system if it exists.
	 *  @param spring The spring to remove.
	 *  @return The particle system updated with the removed spring.
	 */
	public final ParticleSystem removeSpring(Spring spring)
	{
		springs.remove(spring);
		return this; 
	}
	
	/** Reports a list of the attractions currently defined as part of this particle system.
	 *  @return List of attractions.
	 *  @deprecated Replace in favour of the more consistently named getAttractions().
	 */
	public final List<Attraction> attractions() 
	{ 
		return attractions; 
	}
	
	/** Reports a list of the attractions currently defined as part of this particle system.
	 *  @return List of attractions.
	 */
	public final List<Attraction> getAttractions() 
	{ 
		return attractions; 
	}
	
	/** Reports the number of attractions in this particle system.
	 *  @return Number of attractions in the system.
	 *  @deprecated Replace in favour of the more consistently named getNumAttractions().
	 */
	public final int numberOfAttractions() 
	{ 
		return getNumAttractions(); 
	}
	
	/** Reports the number of attractions in this particle system.
	 *  @return Number of attractions in the system.
	 */
	public final int getNumAttractions() 
	{ 
		return attractions.size(); 
	} 
	
	/** Provides the attraction at the given position in the list of attractions stored in this particle system.
	 *  @param i List index (the ith attraction in the list).
	 *  @return The attraction requested.
	 */
	public final Attraction getAttraction(int i) 
	{
		return attractions.get(i); 
	}
	
	/** Removes the attraction at the given position in the list of attractions stored in this particle system.
	 *  @param i List index (the ith attraction in the list).
	 *  @return The attraction removed.
	 */
	public final Attraction removeAttraction(int i)
	{ 
		return attractions.remove(i); 
	}
	
	/** Removes the given attraction from the list of attractions stored in this particle system if it exists.
	 *  @param attraction The attraction to remove.
	 *  @return The particle system updated with the removed attraction.
	 */
	public final ParticleSystem removeAttraction(Attraction attraction)
	{ 
		attractions.remove(attraction);
		return this;
	}
	
	/** Reports a list of the custom forces currently defined as part of this particle system.
	 *  @return List of custom forces.
	 *  @deprecated Replace in favour of the more consistently named getCustomForces().
	 */
	public final List<AbstractForce> customForces() 
	{ 
		return getCustomForces(); 
	}  
	
	/** Reports a list of the custom forces currently defined as part of this particle system.
	 *  @return List of custom forces.
	 */
	public final List<AbstractForce> getCustomForces() 
	{ 
		return customForces; 
	}  
	
	/** Adds a custom force to those in this particle system.
	 * @param customForce Custom force to add.
	 * @return Particle system with the new custom force added.
	 */
	public final ParticleSystem addCustomForce(AbstractForce customForce) 
	{ 
		customForces.add(customForce); 
		return this;
	}
	
	/** Reports the number of custom forces in this particle system.
	 *  @return Number of custom forces in the system.
	 *  @deprecated Replace in favour of the more consistently named getNumCustomForces().
	 */
	public final int numberOfCustomForces() 
	{
		return getNumCustomForces();
	}
	
	/** Reports the number of custom forces in this particle system.
	 *  @return Number of custom forces in the system.
	 */
	public final int getNumCustomForces() 
	{
		return customForces.size();
	}
	
	/** Provides the custom force at the given position in the list of custom forces stored in this particle system.
	 *  @param i List index (the ith custom force in the list).
	 *  @return The custom force requested.
	 */
	public final AbstractForce getCustomForce(int i)
	{ 
		return customForces.get(i);
	}
	
	/** Removes the custom force at the given position in the list of custom forces stored in this particle system.
	 *  @param i List index (the ith custom force in the list).
	 *  @return The custom force removed.
	 */
	public final AbstractForce removeCustomForce(int i)
	{
		return customForces.remove(i);
	}
	
	/** Removes the given custom force from the list of custom forces stored in this particle system if it exists.
	 *  @param customForce The custom force to remove.
	 *  @return The particle system updated with the removed custom force.
	 */
	public final ParticleSystem removeCustomForce(AbstractForce customForce)
	{ 
		customForces.remove(customForce);
		return this; 
	}
	
	/** Reports a list of the particles currently defined as part of this particle system.
	 *  @return List of particles.
	 *  @deprecated Replace in favour of the more consistently named getParticles().
	 */
	public final List<Particle> particles() 
	{ 
		return getParticles(); 
	}
	
	/** Reports a list of the particles currently defined as part of this particle system.
	 *  @return List of particles.
	 */
	public final List<Particle> getParticles() 
	{ 
		return particles; 
	}
	
	/** Reports the number of particles in this particle system.
	 *  @return Number of particles in the system.
	 *  @deprecated Replace in favour of the more consistently named getParticles().
	 */
	public final int numberOfParticles() 
	{ 
		return getNumParticles();
	}
	
	/** Reports the number of particles in this particle system.
	 *  @return Number of particles in the system.
	 */
	public final int getNumParticles() 
	{ 
		return particles.size(); 
	}
	
	/** Provides the particle at the given position in the list of particles stored in this particle system.
	 *  @param i List index (the ith particle in the list).
	 *  @return The particle requested.
	 */
	public final Particle getParticle(int i)
	{ 
		return particles.get(i);
	}
	
	
	/** Removes the given particle from the list of particles stored in this particle system if it exists.
	 *  @param p The particle to remove.
	 *  @return The particle system updated with the removed particle.
	 */
	public final ParticleSystem removeParticle(Particle p)
	{ 
		particles.remove(p);
		return this; 
	}

	/** Clears the particle system of all particles, springs, attractions and custom forces.
	 */
	public final void clear() 
	{
		particles.clear();
		springs.clear();
		attractions.clear();
		customForces.clear();
	}

	/** Applies the forces contained in this particle system to those particles subject to them.
	 */
	protected final void applyForces()
	{
		if (!gravity.isZero())
		{
			for (final Particle p : getParticles())
			{
				p.addForce(gravity).addForce(Vector3D.multiplyBy(p.velocity(), -drag));
			}
		} 
		else 
		{
			for (final Particle p : getParticles()) 
			{
				p.addForce(Vector3D.multiplyBy(p.velocity(), -drag));
			}
		}
				
		for (final Spring f : getSprings())
		{
			f.apply();
		}
		
		for (final Attraction f : getAttractions())
		{
			f.apply();
		}
		
		for (final AbstractForce f : getCustomForces()) 
		{
			f.apply();
		}
	
	}

	/** Removes all forces from this particle system. Unlike <code>clearAllForces()</code>, this
	 *  will maintain the internal collections of springs, attractions and custom forces, but these
	 *  remain unattached to any particular particles.
	 */
	protected final void clearForces() 
	{ 
		for (Particle p : particles)
		{
			p.clearForce(); 
		}
	}
	
	/** Removes all forces, springs and attractions from the particle system.
	 */
	public final void clearAllForces()
	{
		clearForces();
		springs.clear();
		attractions.clear();
		customForces.clear();
	}
	
	// -------------------------------- Private methods -----------------------------------
	
	/** Convenience method for throwing NullPointerExceptions.
	 * @param o the object to test for null
	 * @param message the message to use, if o is null
	 * @throws NullPointerException if o is null, with message
	 */
	private static final void nullThrower(Object o, String message)	throws NullPointerException
	{ 
		if (o==null) 
		{
			throw new NullPointerException(message); 
		}
	}
	
	/** Convenience method for throwing IllegalArgumentExceptions.
	 * @param message the message to use
	 * @return convenience return type for use with ? : ; operator
	 * @throws IllegalArgumentException with message
	 */
	private static final ParticleSystem	illegalArgThrower(String message)
	{ 
		throw new IllegalArgumentException(message); 
	}


}