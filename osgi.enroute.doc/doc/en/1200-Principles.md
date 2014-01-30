# Principles

With the Internet today it is no problem to find a solution to almost any detailed question. It is therefore hard to understand how we worked only a decade ago without Google, Bing, and Stack Overflow at our finger tips. Across my desk there are hundreds of books explaining CVS, XML, XSLT, LDAP, and other old technologies, silently testifying about this Before Google era. However, there are also books in that bookshelf that do not prescribe a solution to a detailed problem but that attempt to go deeper into the mysteries of the software development process. Books that discuss the _principles_ of the software development process without reverting to ephemeral screen shots.

So why are principles important? Most detailed technical problems can nowadays be solved expediently with the help of the millions of peers that are now only a few swipes or mouse clicks away. Principles are important because they guide us. Software is a frighteningly complex endeavor that forces its practitioners to make hundreds to thousands of decisions each day; decisions that can actually have quite far reaching consequences. Though each of these decisions has a local optimum, it is not that local optimum that creates great systems. You cannot look at the millions of parts in a 747 airplane and point out a single part that makes it fly. Flying is the combined result of all those millions of parts _interacting_. To make these parts work, they need to be _designed_. To make these designs work together, we need an _architecture_ that defines the rules and constraints of the designs. To define an architecture we need principles that guide us in developing the right architecture. 

This chapter therefore outlines the principles in software development that are used in the enRoute project. These principles provide the guidelines for the enRoute architecture.

## Black and White

In software a situation is rarely black and white. In almost all cases there are many forces at play that make some solutions more desirable than others. In the upcoming chapters the different aspects of software development are discussed. Not with the intent to provide black  and white rules but only with the hope that the forces for that aspect are understood and recognized.

This chapter is about grey and and there will be many cases of 'on the other hand'. There are actually a number of contradictions in this text because they do require trade offs that must be understood. In the architecture chapter we will make concrete choices. 

## Time

Trying to explain our industry to lay people is hard. It is hard because what we software developers are doing has remarkably little to do with the concrete world; cyberspace is truly a different area. We use words like _build_, _object_, and _framework_ that are defined in a concrete world but have much more ephemeral semantics in our virtual world. You build a house from concrete, stones, and wood, a far cry from flipping bits on a hard disk in what we call the build process. Objects are, yeah, what are objects actually? And where a real framework is touchable, our frameworks are intangible. No wonder that many are at large when we try to describe to our partner what it is we do. since we utterly confuse them with these inadequate metaphors.

The hardest aspect to explain aspect of our work is the volatility. The baker bakes bread, and the bricklayer builds buildings. They deliver a concrete result to their customers and the next day they bake or build something brand new, unrelated to yesterday's work. Software engineers 'build' their 'software' several times a day, but they seem to deliver largely the same thing over and over to their customers. It is interesting to see how we do not even have proper terminology in our industry. In [maven][4] we talk about an artifact but it is not clear if refers to the bits on disk (the JAR file), or the project that builds it, or something maybe even something else? In this document we use _program_ for the what is the combination of _groupId_ and _artifactId_ in maven and _revision_ when a specific _version_ is added. The term _project_ defines the concept of a set of artifacts that can be used to _build_ a revision.

The difficulty of describing these core development processes clarifies why explaining to your parents what you do day in and out is hard. The core of our business is a long lasting process of reshuffling bits so that when they are combined with computers and users we achieve the results we promised. We call this process 'maintenance' but it has very little to do with the maintenance in the real world. In the real world, products deteriorate. A car needs an oil change or certain parts are designed to wear out over time and need to be replaced before they pass a breaking point, causing great damage. Bizarrely, in software we theoretically do not have wear and tear since a bit is a bit and they do not chance happenstance. A revision is immutable for all time. What we call 'maintenance' is actually a different process. In this process we:

* Fix bugs, which we have at a rate that surprises me that it is expected by the public at large.
* Add features, which we often give away for free.
* Adapt to changes in the sytem's context, which is also called addressing the _bitrot_ effect. Despite the fact that revisions do not change, their eternally evolving context causes the revision to become maladjusted over time.

It should be clear that a large part of our work is addressing the effects time. The context changes, which requires us to change the software, which changes the context for others.When we develop software we should be aware at any time that we are not building but in a continuous shaping process. It is crucial to be aware that any real world system lives in an ever evolving context where our own changes contribute to this changing context.

There are many practices in our industry that would make sense if not for the case that we should prepare for continuous change without termination. This clearly means that for example versioning is important to be able to distinguish revisions but there are more subtle consequences. 

A surprising example is _aggregating_, putting parts together in a greater whole, or relying on a dependency. For example, you repackage a number of JARs in a bundle. Every time you aggregate a set of parts, you first create an additional responsibility because the underlying artifacts, the dependencies, will change many times.  Each change will add some maintenance costs. Also, you have to make the aggregation evolve at the rate of its fastest evolving part or the clients of the fastest part will be upset. Last but not least, you now constrain the revisions of the constituents as they are in the aggregate, it becomes harder to use an independent part on its own. Though an aggregate or repackiging can have benefits, see [Modularity], the drawbacks of increasing the rate of evolution and the additional constraints between the parts do have a cost due to the continuous changing world that must be taken into account.

To problems around aggregation are illustrated by the concept of _profiles_. A profile is a set of API revisions put together so that end users can have a single JAR to compile against. In the Java world there are a number of J2ME profiles, and of course Java SE and Java EE can also be seen as profiles when squinting a bit. Developers in general love them because it seems to simplify their lives considerably. That is, until they find out there is a newer version of a profile's constituent that they absolutely need or when it is time to add new parts and they find that the process of maintaining the profile is highly politicized since there are now many different interests to take into account. In the 90's Ericsson and HP had TMOS, a Telecom Management Operating System, that imploded because they found it impossible to release a revision that satisfied the needs of all their users.    

With respect to time, we should then take the following principles into account:

* Versioning – Ensure that independent parts are versioned so that tracing and matching can be automated.
* Prepare for change – Ensure that the code base is always optimal for additional changes since they will happen.
* Minimize the cost of change – Since things will change ensure that when change happens the impact, and thus the cost, is minimal.
* Understand the impact of changes – Assume that things change, what is the effect on additional parts in your projects.

## Less is More <=>

The [mathematical theory of complexity][6] is surprisingly, ehh, complex. Worse, it seems utterly useless for understanding or explaining software complexity. It seems the best measure of software complexity is the good old simple _size_, although with some caveats. Officially called _lex parsimoniae_. Or poetically phrased: 'Less is more'. In general a solution with fewer parts should be preferred over an equally valuable solution that uses more parts. This is [Ockham's razor][7] revisited for software instead of hypotheses. After all, software does resemble a set of hypotheses.

The caveats are also poetically described, attributed to [Einstein][8], that 'Everything should be as simple as possible, but not simpler'. Or as the witticism: 'Every complex solution has a simple solution, that is plain wrong'.

It is hard to describe where the balance is but in general it is best to err on the side of _conciseness_. This does not mean that shorter variable names are better or [packing ten statements in one line][9] will decrease the complexity of the software. However, given two equivalent functions but one larger than the other, then the smaller should have preference.

A tool to keep things small is to [Do Not Repeat Yourself]. Ensure that each concept, fact, item, function, is only defined once and use a reference in other places. If several functions can share a common block, factor out this commonality. If you can reuse classes with small tweaks, do it. Your later readers and maintainers will love you for it.

If the same thing can be expressed more concisely, then the difference is _cruft_. Cruft is the extra text around the stuff that really matters. Some languages, XML is a key example, are often up to 97% redundant. you can verify this by compressing a large XML file. Though a certain amount of redundancy can help understanding, it quickly becomes distracting because we humans have limited cognitive capabilities. 

An important part why size is so relevant for complexity is our eyes and our mind. We humans have a limited cognition, it is assume we can handle about [7±2 'things'][10]. Actually, it is likely that this is 3 groups of 3 things. That is, up to about 3 matches do not require counting since you almost immediately see how many there are on the table. More than 3 matches tend to be grouped by us, so 9 matches would be seen as 3 groups of 3 matches. This effect is called _subitizing_.

Over the 7±2 limit we need to _chunk_. Chunking means combining 'things' in higher level groups. In general we are good in working with large sets of things. For example, we all know the western alphabets that have between 25 and 30 characters. By chunking together letters we can create books that can convey highly complicated material. Our numbers specifically have comma separators for thousands so we can more easily recognize the chunks. That is, 10000000 is hard to recognize as ten million but for 10,000,000 it does not require any effort. Looking at the Japanese and Chinese alphabets that have between a few thousands upto 80.000 characters it is clear that humans can handle very large sets despite their limitation of 7±2 members simultaneously. 

A good example is the American telephone number. Since it consists of ten digits it falls outside the range of what we humans are comfortable with. By breaking the number up in the area code (3 digits), exchange (3 digits), and line number (4 digits) we can remember it much easier. For example, 433-555-7866. And no, [555 exchanges][11] are no longer exclusively reserved for fictional use! 

In our industry we have been chunking since we left the switches on the first computers. Octal and Hexadecimal numbers took initially advantage of chunking. Assembly and higher level languages also chunked lower level concepts (microcode) into higher level concepts so they became easier to work with. Objects, and packages are higher level concepts that group underlying code. All with the effect that it becomes easier to reason about a software system. And why some code bases are really hard, they usually have many more than nine bundles, or nine packages per bundle, or nine objects per package. This is often inevitable but it should be realized that keeping the number of things low is a significant factor in simplifying.   

It should be obvious from this discussion why [Modularity] is so crucial. Modularity can limit the number of simultaneous concepts that need to be considered before a change can be made. Modularity is the chunking mechanism of software.

With respect to size, we should take the following into account:

* Less is more, in general
* Optimize for readability, small is beautiful
* Use chunking to keep the number of chunks that one needs to have in mind understanding a part to less than ten. This is in general why we have functions, objects, packages, and bundles.  

## Dependencies

Dependencies are the Jekyll & Hide of software. The good Dr. Jekyll gives us the advantages of reuse, of not reinventing the wheel, and using less memory because we can share our dependencies. The bad Mr. Hide then downloads the Internet behind our back because all our dependencies also trusted Dr. Jekyll and thought it was a good idea to depend on other stuff. To make life worse, Mr. Hide creates constraints on what we can actually combine that are hidden from view and bite us at unexpected times.

Most developers create software where dependencies are _implicit_, the original coder assumes that somewhere down the line there is a _deployer_ that puts all the dependencies together. The deployer is the role that takes a set of JARs, puts them on the class path and the tries to run the code. When one of the coder's assumptions are violated we see Class Not Found and other Exceptions, or in some cases nothing happens. Classic Java lacks a mechanism to declare dependencies and the result is that it often takes numerous attempts to find a working combination, usually by adding more JARs. A working combination that can still hide lots of problems because nobody knows what assumptions could potentially be violated. 

This situation is often described as _JAR Hell_ (which comes from DLL hell, which arguably makes Microsoft look better, at least in the rhyming). JAR hell is the situation where you end up with incompatible versions of the same library on the class path. This problem is more extensively explained in [Namespaces] but it comes down to the fact that two different JARs both depend on the same program but in a different revision. In classic Java this problem is unsolvable because Java will always return the first class for a given name it finds, it cannot distinguish between classes from different revisions. It should be clear that adding additional JARs on your class path can seriously degrade the situation since these additional JARs carry additional dependencies that can potentially conflict.

[Maven][13] was a major step towards handling dependencies. Maven's dependency mechanism based on the JAR file, it gave the program an identity (the group Id  the artifact Id) and added a version so it could handle multiple revisions, which in general are JAR files. Each program file had a _pom.xml_ file  that expressed the metadata and dependencies on other revisions. Since revisions had unique identities, they could now be stored in a repository, i.e. Maven Central.

Maven dependencies are _transitive_. That is, if you depend on another program you will get all its dependencies, and the dependencies of all those dependencies, ad nauseum. Transitive dependencies quickly add up and a common complaint at Maven is that it 'downloads the Internet'. That is not fair, Maven does not download the Internet, people download the Internet. It is the liberal use of dependencies that people add to their programs, combined with the transitivity that causes the excessive downloads. 

Another problem with Maven is that it only handles dependencies on other programs; it has no mechanism to specify any other type of dependency. Unfortunately, code dependencies is only one of the myriad of assumptions coders encode in their code.

 
In the OSGi we have a very general dependency mechanism called the _Requirement-Capability_ model (RC). This RC model can 

To understand why and when dependencies should be used it is important to understand why not all dependencies are created equal, although in the OSGi we hav.


In general, we need to worry about the following types of dependencies:

* Code – For our code to run, all the code that it will call transitively must be present or it will fail. 
* 

The most common dependency is a _library_. A library is a piece of code with no internal state and a public API. A library can be used by many different components; these components should not be aware of of each other. A library combines its public API with the implementation. That is, a pure library will not require a factory to create instances since implementation and public API fall together. Examples of libraries are ASM, the byte code analyzer and weaving library, Jackson the JSON (de)serializer library, and many others. Libraries cannot be substituted without a significant code change of its users.

If a component has a state that is observable by other components than we call it a _service component_. A service component maintains its state internally and exposes a public API through a _µservice_. That is, a dependency is not on the implementing component but on the µservice. The public API of a service should be formally separated from an implementation to allow for substitution. Service components have two major purposes: to provide abstractions to underlying resources and to allow different implementations for the same functionality. In classic Java service components would be represented with the Service Loader. In OSGi, they are first class citizens. Examples of a service components are the Log service (abstracts the system log), Event Admin service (hides how events are handled) and the Configuration Listener service that allows clients to receive configuration events. [Services] will be extensively discussed later. 

The third type of component causes no direct code dependencies but provides, often crucial, functions to other components. These are called _extender components_. Extender components can watch the life cycle of other components and use resources from these components to act on behalf of them. Extenders provide a mechanism allows components to concentrate on their core business and not carry any unnecessary cruft. Examples of extender components the Blueprint specification, the enRoute Base support, and others. Though there is no direct dependency on an extender component, a component will not work when its extender components it relies on are not available.

The last 



 
The advantages of depending on another component are quite clear:

* Memory – Less memory usage since a component is only stored once and used many times (both core memory and persistence)
* State – Sharing of state. Dependencies that provide an abstraction of a state must, by definition be shared.
* Abstraction 


Using dependencies requires a balance between the cost of a dependency and the benefit. Let us first discuss some of the unexpected problems that arise due to dependencies.





## Standards

Why do standards work in theory? Standards are primarily beneficial because they _decouple_ the components based on the specifications. Each component is aware of the middle man (the standard) but can be oblivious of other components that leverage the same standard. Components can therefore be brought together by the _deployer_. This clearly minimizes communications between parties and thereby reduces errors. Instead of having everybody talk to everybody (which is exponential) the amount of communications is reduced to everybody talking to the standards body (which is linear). 

To discuss standards further it is necessary to introduce a bit of terminology. A standard decouples the implementation that _provides_ an implementation of a standard from the implementation  that _consumes_ this standard. For example, [Equinox][1] is an implementation of the [OSGi Core Framework Specification][2], that is, it is a _provider_ of the OSGi Framework API. A bundle that uses the framework API is a _consumer_.

A provider is the party that is responsible providing the _contract_ specified in the specification. A consumer is the party that uses the contract. A common confusion is that a provider always implements an interface, however this is not always true, a provider can also use interfaces that must be implemented by the _consumer_. For example, in the OSGi Framework API the `BundleActivator` interface is implemented by the consumer, not the provider. There make sure not to confuse the provider with implementer of an interface and the consumer with the user of an interface. Both providers and consumers use and implement interfaces.

The reason we need these different terms, provider and consumer, because even though they rely on the same specification there roles are quite different. If the specification changes, the provider must always be changed to match the new features; a provider has very little to no backward compatibility with respect to a specification. For example, every release the OSGi Alliance has for the OSGi Framework requires Equinox and [Felix][3] to adapt their code bases to provide the new features. In contrast, a consumer usually gets extensive backward compatibility; specification bodies tend to go out of their way to keep new releases of software backward compatible for the API consumers. For example, a bundle written for OSGi Release 1 likely still works after 16 years and 8 releases later since most of the APIs have been evolved in a backward compatible way for consumers. The [Semantic Versioning] principles outline the use of versioning, that is, how to manage an evolving code base.  

However, there are a number of other _theoretical_ advantages of well designed specifications:

* Testability – A standard API must be decoupled from implementation details. This makes them easier to test since the APIs should be uncoupled. This will keep the required mocking of test objects low.
* Robustness – Since there are more implementations, the changes are that the API is more heavily used and thereby tested.
* Quality –  Competition, enabled by multiple implementations, should help to increase the quality.
* Documented – The specification provides documentation for the implementations. Documentation is often a weak part of many implementations.  
* Eyes – A specification will get many eyes, this increases the quality of the API.
* Stability – Specifications tend to evolve at a slower rate than implementations. This makes the development process more stable.

Not all these advantages are always met since not all specification processes are created equal. In the Java world we mainly have the [Java Community Process (JCP)][5]. In theory this should be Java's greatest asset but in practice the process provides too much leeway to the vendors (euphemistically called specification leads) to follow their own interests. The process deliberately does not specify the deliverables, creating a hodge-podge of specifications of varying quality. Though vendors are important in this process because  only they can provide the resources to develop good specification; users of the specifications can rarely afford this kind of involvement.

Clearly, standards have the promise of many benefits but bad standards can be very costly. When selecting a specification, pay close attention to the following aspects:

* A specification document documenting the non-syntactic aspects of the API
* A set of Java classes and interfaces that can be used by developers. These API classes should be delivered in binary readable form (generally a JAR) so that the compiler can be used to verify the syntactic constructs of the API. To prevent pollution from implementation details, these JARs should **not** contain the reference implementation, which is common in the JCP. 
* Javadoc to help with the usage of the API and provide information about non-syntactic aspects.   
* A reference implementation to prove that the specification is implementable
* A thorough test suite that treats the API as a black box and can be used to verify the specified behavior
* A community so that developers can ask questions

## Open Source

The holy grail of software has always been reusability: building systems out of reusable components. From a certain perspective this grail has been achieved today, there is an amazing amount of gratis software freely available. Maven Central is growing at about 5% a month and was already at more than 521.000 JARs at January 2014. In this repository you can find perfect gems as well as a boatload of crap. 

Open source has the following advantages:

* Gratis – In general open source code is freely available. At least there is no fee to obtain it. This makes it easy to bypass management or procurement.
* Handling – Since the software can be freely downloaded it is possible to check it out without requiring permission. In general this significantly simplifies evaluation.  
* Eyes – Popular open source projects have multiple people looking at the code. This increases the confidence one can have about the quality.
* Longevity – Open source software is likely to stay, or at least linger, around forever. One of the reason Airbus is using open source, and provides software as open source, is because they have requirements lasting 80 years. Open software is one of the few areas that can provide such an availability guarantee.
* Community – If there is a community around an open source project than it is easier to find knowledgeable people; this makes it easier to find answer to problems on StackOverflow or other places. Any issues will also quickly become clear due to discussions on the Internet about this. If things go badly, it is also less painful to be in company.

That said, open source software is not without cost and disadvantages:

* Indeterminate Quality – Though there are many open source projects that provide solid quality control, there are many more that have no controls. It is up to all users of the open source projects to decide what the quality is, and if this is sufficient.
* Governance – Using open source software can be a black art if it comes to licensing issues. Some software is very liberal, other software has stringent requirements that can affect the visibility of your own code (GPL) that relies on this software. Patents in the software can also affect your bottom line. Understanding the implications is hard. Several companies like [Sonatype][12] and [Blackduck][13] have sprung up to help you with the implications of open source. However, the cost for these companies changes the gratis picture of open source.
* Responsibility – Popular open source projects often have numerous committers and really popular projects have dedicated companies providing patches to existing projects for special needs. However, this can be very expensive. For dying projects it is sometimes necessary to clone a repository and take over the maintenance. This is obviously an unwanted situation since it combines the worst of both worlds by giving you the responsibility of a foreign code base.
 
## Don't Repeat Yourself

## Modularity

## Services First

## Name Spaces

## Reuse

## Ignorance is Bless

## Cohesion, Separation of Concerns
http://programmer.97things.oreilly.com/wiki/index.php/The_Single_Responsibility_Principle

## Coupling, Aggregation



## Automate
beware of warnings, errors

## Iterative

## Reuse and Sharing Pitfalls
http://programmer.97things.oreilly.com/wiki/index.php/Beware_the_Share

## Exceptions

## Maintenance

## Refactoring

## Versioning

## Singletons

## Serve the Deployer

## 


[1]: http://www.eclipse.org/equinox/
[2]: http://www.osgi.org/Specifications/HomePage
[3]: http://felix.apache.org/
[4]: http://maven.apache.org/
[5]: https://www.jcp.org/en/home/index
[6]: http://en.wikipedia.org/wiki/Kolmogorov_complexity
[7]: http://en.wikipedia.org/wiki/Occam's_razor
[8]: http://quoteinvestigator.com/2011/05/13/einstein-simple/
[9]: http://www.ioccc.org/
[10]: http://en.wikipedia.org/wiki/The_Magical_Number_Seven,_Plus_or_Minus_Two
[11]: http://en.wikipedia.org/wiki/555_(telephone_number)
[12]: http://www.sonatype.com/
[13]: http://www.blackducksoftware.com/
