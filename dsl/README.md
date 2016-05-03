#Meta Model DSL

KMF comes with a simple, yet powerful textual domain-specific language for defining meta models.
The language allows to define concepts like classes, enums, attributes, relationships between classes, functions, and machine learning strategies.
In the following we will describe the meta model language step by step.      

Reserved keywords of the language are: *class, att, rel, extends, with, func, dependency, input, output*.    

**Classes**: are the main structural concept of meta models. 
Following the object-oriented programming paradigm a class contains attributes and functions and represents a domain concept. 
The example below shows a simple class declaration of a class *smartgrid.SmartMeter*: 

```java
class smartgrid.SmartMeter {
    // attributes 
    // functions
}    
```	

In order to avoid naming conflicts, class names should be fully qualified.  

**Enumeration values**: are like classes first level concepts. 
They are specified in a similar way than classes. 
An example for a typical enum type are the days of a week:

```java
enum smartgrid.WeekDay {
    monday, tuesday, wednesday, thursday, friday    
}    
```	    

**Attributes**: are specified using the *att* keyword followed by a unique (within the scope of the class) *name* and a *type*. 
Valid types for attributes are the primitive types *String, Long, Bool, Int, Double*, *enum* values, *continuous* types, and arrays of all of these types.
Attributes must be always defined within the scope of a *class*.
Following example defines an attributes with name *serialNumber* of type *String* and another attribute with name *modelType* of type *SmartMeterModel*.

```java
     class smartgrid.Meter extends smartgrid.Entity {
         att serialNumber: String
         att modelType: smartmeter.SmartMeterModel 
     }    

```	   

**Relationships**: can be specified between classes.
A relation is defined by its name (must be unique within the scope of a class) and a type (the class to which the relation points to).
By default, every specified relation is an unbounded to-many relation.
Relationships can be refined by defining a maximum bound by adding a *with maxBound* to the relation specification.
In case a relationship should be navigable in both directions (source and target class), a relation must be specified in both classes where one side needs to defined as the *opposite* of the relation.  
This is shown in the next example:

```java
     class smartgrid.Meter extends smartgrid.Entity {
         att serialNumber: String
         att modelType: smartmeter.SmartMeterModel
          
         rel customer: smartgrid.Customer with maxBound 1 with opposite "meter"
     }    

    class smartgrid.Customer {
        rel meter: smartgrid.Meter with opposite "customer"
    }

```	   

In this example, the *smartgrid.Meter* class defines a (bidirectional) relationship to the class *smartgrid.Customer*. 
The relationship is refined as a to-one relation using the *with maxBound 1* restriction.
To make the relationship bidirectional (navigable from both sides) we declare a relationship *meter* from *smartgrid.Customer* to *smartgrid.Meter* and define it as the opposite of the relation *customer* from *smartgrid.Meter* to **smartgrid.Customer*.   

**extends**: Like in many object-oriented programming languages, classes in KMF meta models can *extend* (inheritance) other classes. 
In this case, the child class inherits all attributes, relationships, and functions from the parent class:
 
```java
    class smartgrid.Meter extends smartgrid.Entity {
    }
```
	 
In this example the class *smartgrid.Meter* inherits all attributes, relationships, and functions of *smartgrid.Entity*. 	  

**Indexed attributes**: the structure of KMF leads to a graph of objects. 
In order to read this graph of data, man have to identify some entry-points and naviguates other objects through relationships traversal operations.
These entry-points are in fact objects that are indexed according to their attributes values.
These indexes are costly and therefore are not done by default, therefore in KMF, objects that can act as entry-points should be identify first in the domain.
To do so, we offer in the DSL definition a special attribute annotation named `index` that allow to define that this attribute will be part of the *key* to identify one object of this specific metaclass.
The following example highlight this usage.

```java
    class smartgrid.Meter {
    	att name: String with index
    }
```

In this code snippet we define that any smartMeter object can be identify thourgh it's name.
We also define that no duplicate smartMeter object can share the same name (the last defined one will silently override the previous one).
Index are automatically filled while these special attributes are manipulated.

```java
meter0 = model.createSmartMeter(0,0);
meter0.setName("Meter0");
```

Now this object are reacheable using the name and a TIME and UNIVERSE number. As the following:

```
model.findByName("SmartMeter",0,0,"name=Meter0");
```

Finally, indexes are relative to time and universe, as any element in KMF. Therefore if an object is modified in future, the index will be altered only in future. Therefore, the refactoring does not apply for past elements.

```
meter0.jump(10,{ meter10 ->
	meter10.setName("SmartMeter0");
}
```

Now the find method will retrieve the object with the name SmartMeter0 *AFTER or EQUALS* time 10 and with the name Meter0 for time *LOWER* than 10.

Finally it feasible to use several attribute as the key. Such as:

```java
    class smartgrid.Meter {
    	att prefix: String with index
    	att name: String with index
    }
```

Then, to retreive the same object the query will look like:

```
model.findByName("SmartMeter",0,0,"prefix=ORG,name=Meter0");
```

**WARNING:**
* An object without index or without any link (directly or not) with an indexed object is **NOT reachable**. Thus, it is not possible to recover it. (Unless you keep in memory its ID)
* KMF uses an Hash Indexes. So, only the egual operation can be used in a query.
* A class with multiple indexed attribute should be understood as a "multi-column" index. In the previous example, the hash is computed from the concatenation of the `prefix` and the `name` of `smartgrid.Meter`. Thus, to retrieve the object the exact `prefix` AND exact `name` are required.

**Functions**: define, like in object-oriented programming, the possible behaviour of classes. 
Functions are defined using the keyword *func* followed by a *name*, a list of *parameters*, and *return type*. 
Parameters and return types can be primitive types (*String, Long, Bool, Int, Double*), *enumeration values*, *classes*, as well as arrays of all of these types.
The following example shows a class *smartgrid.SmartMeter* with three functions. 
The first one, *register*, takes no parameters and doesn't return anything.
The second function, *searchConcentrator*, takes no parameter but returns a *smartgrid.Concentrator*. 
Finally, the third function, *myFunc*, takes two parameters, an array of *Strings* and a *smartgrid.Concentrator* object. 
The function returns a *smartgrid.SmartMeter* object. 

```java
    class smartgrid.SmartMeter extends smartgrid.Entity, smartgrid.Meter {
        func register
        func searchConcentrator : smartgrid.Concentrator
        func myFunc(s: String[], c: smartgrid.Concentrator) : smartgrid.SmartMeter 
    }
``` 

**Continuous attributes**: by nature, some values like temperature or time are not discrete but continuous.
Usually, these values are artificially 'discretized', e.g., by taking regular samples and store them together with a timestamp. 
This, however, means that there will be a gap between two samples. 
Moreover, when sampling at a very high rate this requires lots of storage capacity. 
Therefore, attributes in KMF can be declared as *continuous*. 
Continuous attributes are stored as a function instead of single values. 
An accepted error rate, which must be specified together with the attribute, decides the function which is used to represent the values.
The following example shows the *smartgrid.Consumption* class specifying a *continuous attribute* named *activeEnergyConsumed* with a *precision of 0.9*.  
This means that the value defined by the function can maximal derive from the discrete value by *0.9*.

```java
    class smartgrid.Consumption {
        att activeEnergyConsumed: Continuous with precision 0.9
    }
``` 

**Machine learning strategies**: KMF allows to describe important dependencies between meta class concepts and to specify different learning strategies based on these dependencies.
Let's consider a concrete example. 
The idea is, that the default consumption behaviour of a customer (its smart meter) can be learned based on his consumption values.
Therefore, we first define a relationship from *smartgrid.SmartMeter* to its profiler class *smartgrid.ConsumptionProfiler*. 
This enables us to monitor the consumption on a per *smartgrid.SmartMeter* object. 
 
```java
    class smartgrid.SmartMeter extends smartgrid.Entity, smartgrid.Meter {
        rel profiler: smartgrid.ConsumptionProfiler with maxBound 1
    }
``` 

Next, we define the *smartgrid.ConsumptionProfiler* class.

```java
    class smartgrid.ConsumptionProfiler {
        with inference "GaussianProfiler" with temporalResolution 2592000000
    
        dependency consumption: smartgrid.Consumption
    
        input timeValue "@consumption | =HOURS(TIME)"
        input activeEnergyConsumedValue "@consumption | =activeEnergyConsumed"
    
        output probability: Double
    }
``` 

The algorithm used for the machine learning has to be specified with the key word *with inference* followed by the name of the machine learning algorithm, in this case *GaussianProfiler*.
Currently, the following algorithms are supported in KMF: *BinaryPerceptron, LinearRegression, KMeanCluster, GaussianProfiler, GaussianClassifier, GaussianAnomalyDetection, Winnow, EmptyInfer*.
Depending on the algorithm a time resolution can be specified using the keyword *with temporalResolution* followed by a number giving the time in milliseconds.
In this example, this specifies the time over which the Gaussian profiler learns. 
In addition, we need to specify the *dependency*, meaning on what values the profiler should work on.
This is done in the example by specifying the name and type of the dependency: *dependency consumption: smartgrid.Consumption*.
The profiling in our example is time based and depends on the *activeEnergyConsumedValue* attribute of the *smartgrid.Consumption* class.
Therefore, we first specify a *timeValue* input, which is available for all objects in KMF.
The *@consumption* declares that the time value should be taken from the *consumption* class. 
*|* can be interpreted as pipe. 
*HOURS(TIME)* is a convenient function provided by KMF, which extracts the hours from the time attribute (which is by default in milliseconds). 
Next, we specify the attribute *activeEnergyConsumed* from class *consumption* as second input for the learning algorithm and define *activeEnergyConsumedValue* as name for the input.
Last but not least, we define the output of the profiler, a *Double* value with name *probability*.
