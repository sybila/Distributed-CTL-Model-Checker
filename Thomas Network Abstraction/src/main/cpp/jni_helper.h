#include <vector>
#include <list>
#include <iostream>

class JVM {

	template<class T>
	class AnyInstance {
	protected:
		jobject _value;
		T * _type;
	public:
		AnyInstance(jobject value, T * classType) : _value(value), _type(classType) {}
		jobject object() { return _value; }
	};

	class AnyClass {
	protected:
		JNIEnv * _env;
		JVM * _jvm;
		jclass _class;
	public:
		AnyClass(JVM * jvm, const char * className) : 
			_jvm(jvm), 
			_env(jvm->_env),
			_class(_env->FindClass(className)) {};
	};

	JNIEnv * _env;

public:

	JNIEnv * getEnv() {
		return _env;
	}

	class DoubleClass : public AnyClass {
		jmethodID _valueOf;
		jmethodID _doubleValue;
	public:
		class Instance: public AnyInstance<DoubleClass> {
		public:
			Instance(jobject value, DoubleClass * type) : AnyInstance<DoubleClass>(value, type) {}
			double doubleValue() {
				return _type->_env->CallDoubleMethod(_value, _type->_doubleValue);
			}
		};
		DoubleClass::Instance valueOf(double d) {
			return DoubleClass::Instance(_env->CallStaticObjectMethod(_class, _valueOf, d), this);
		}
		DoubleClass( JVM * jvm ) : AnyClass(jvm, "java/lang/Double") {
			_valueOf = _env->GetStaticMethodID(_class, "valueOf", "(D)Ljava/lang/Double;");
			_doubleValue = _env->GetMethodID(_class, "doubleValue", "()D");
		}
	};

	class IntegerClass : public AnyClass {
		jmethodID _valueOf;
		jmethodID _intValue;
	public:
		class Instance: public AnyInstance<IntegerClass> {
		public:
			Instance(jobject value, IntegerClass * type) : AnyInstance(value, type) {}
			double intValue() {
				return _type->_env->CallDoubleMethod(_value, _type->_intValue);
			}
		};
		IntegerClass::Instance valueOf(int i) {
			return IntegerClass::Instance(_env->CallStaticObjectMethod(_class, _valueOf, i), this);
		}
		IntegerClass(JVM * jvm) : AnyClass(jvm, "java/lang/Integer") {
			_valueOf = _env->GetStaticMethodID(_class, "valueOf", "(I)Ljava/lang/Integer;");
			_intValue = _env->GetMethodID(_class, "intValue", "()I");
		}
	};

	class ListClass : public AnyClass {
		jmethodID _add;
		jmethodID _get;
		jmethodID _size;
		jmethodID _constructor;
		jclass _arrayList;
	public:
		class Instance : public AnyInstance<ListClass> {
		public: 
			Instance(jobject value, ListClass * type) : AnyInstance(value, type) {}
			jboolean add(jobject item) {
				return _type->_env->CallBooleanMethod(_value, _type->_add, item);
			}
			jobject get(jint i) {
				return _type->_env->CallObjectMethod(_value, _type->_get, i);
			}
			jint size() {
				return _type->_env->CallIntMethod(_value, _type->_size);
			}
		};
		ListClass(JVM * jvm) : AnyClass(jvm, "java/util/List") {
			_arrayList = _env->FindClass("java/util/ArrayList");
			_get = _env->GetMethodID(_class, "get", "(I)Ljava/lang/Object;");
			_add = _env->GetMethodID(_class, "add", "(Ljava/lang/Object;)Z");               
			_size = _env->GetMethodID(_class, "size", "()I");
			_constructor = _env->GetMethodID(_arrayList, "<init>", "()V");
		}
		ListClass::Instance create() {
			return ListClass::Instance(_env->NewObject(_arrayList, _constructor), this);
		}
	};

	class MapClass : public AnyClass {
		jmethodID _put;
		jmethodID _get;
		jmethodID _size;
		jmethodID _init;
	public:
		class Instance : public AnyInstance<MapClass> {
		public: 
			Instance(jobject value, MapClass * type) : AnyInstance(value, type) {}
			jobject put(jobject key, jobject item) {
				return _type->_env->CallObjectMethod(_value, _type->_put, key, item);
			}
			jobject get(jobject key) {
				return _type->_env->CallObjectMethod(_value, _type->_get, key);
			}
			jint size() {
				return _type->_env->CallIntMethod(_value, _type->_size);
			}
		};
		MapClass(JVM * jvm) : AnyClass(jvm, "java/util/Map") {
			_get = _env->GetMethodID(_class, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
			_put = _env->GetMethodID(_class, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
			_size = _env->GetMethodID(_class, "size", "()I");
		}
	};

	
	class ColorSetClass : public AnyClass {
		jmethodID _createFull;
		jmethodID _unset;
	public:
		class Instance : public AnyInstance<ColorSetClass> {
		public:
			Instance(jobject value, ColorSetClass* type) : AnyInstance(value, type) {}
			Instance(TransConst transition, int offset, int size, ColorSetClass * type) : 
				AnyInstance(type->createFull(size), type) 
			{
				for (int i = 0; i < transition.targets.size(); ++i)
				{
					if (
						(transition.req_dir && transition.targets[i] <= transition.comp_value) ||
						(!transition.req_dir && transition.targets[i] >= transition.comp_value)
					)	//if we want increase but are decreasing or vice versa
					{
						this->unset(i + offset);
					}
				}
			}
			void unset(jint i) {
				_type->_env->CallVoidMethod(_value, _type->_unset, i);
			}
		};
		ColorSetClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/thomas/BitMapColorSet") {
			_unset = _env->GetMethodID(_class, "unset", "(I)V");
			_createFull = _env->GetStaticMethodID(_class, "createFull", "(I)Lcz/muni/fi/thomas/BitMapColorSet;");
		}

		jobject createFull(jint size) {
			return _env->CallStaticObjectMethod(_class, _createFull, size);
		}
	};


	class NodeClass : public AnyClass {
		jmethodID _addSuccessor;
		jmethodID _addPredecessor;
	public:
		class Instance : public AnyInstance<NodeClass> {
		public:
			Instance(jobject value, NodeClass * type) : 
				AnyInstance(value, type) {}
			void addSuccessor(NodeClass::Instance node, ColorSetClass::Instance colors) {
				_type->_env->CallVoidMethod(_value, _type->_addSuccessor, node.object(), colors.object());
			}
			void addPredecessor(NodeClass::Instance node, ColorSetClass::Instance colors) {
				_type->_env->CallVoidMethod(_value, _type->_addPredecessor, node.object(), colors.object());
			}
		};
		NodeClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/thomas/LevelNode") {
			_addSuccessor = _env->GetMethodID(_class, "addSuccessor", "(Lcz/muni/fi/thomas/LevelNode;Lcz/muni/fi/thomas/BitMapColorSet;)V");
			_addPredecessor = _env->GetMethodID(_class, "addPredecessor", "(Lcz/muni/fi/thomas/LevelNode;Lcz/muni/fi/thomas/BitMapColorSet;)V");
		}
	};

	class NodeFactoryClass : public AnyClass {
		jmethodID _getNode;
		jfieldID _variableOrdering;
		jfieldID _paramSpaceWidth;
	public:
		class Instance : public AnyInstance<NodeFactoryClass> {
		public:
			ListClass::Instance variableOrdering;
			Instance(jobject value, NodeFactoryClass * type) : 
				AnyInstance(value, type),
				variableOrdering(_type->_env->GetObjectField(_value, _type->_variableOrdering), &(_type->_jvm->List)) {

			}

			void setParamSpaceWidth(int width) {
				_type->_env->SetIntField(_value, _type->_paramSpaceWidth, width);
			}

			NodeClass::Instance getNode(jintArray coordinates) {
				jobject obj = _type->_env->CallObjectMethod(_value, _type->_getNode, coordinates);
				return NodeClass::Instance(obj, &(_type->_jvm->Node));
			}
		};
		NodeFactoryClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/thomas/NetworkModel") {
			_getNode = _env->GetMethodID(_class, "getNode", "([I)Lcz/muni/fi/thomas/LevelNode;");
			_paramSpaceWidth = _env->GetFieldID(_class, "paramSpaceWidth", "I");
			_variableOrdering = _env->GetFieldID(_class, "variableOrdering", "Ljava/util/List;");
		}
	};

	DoubleClass Double;
	IntegerClass Integer;
	ListClass List;
	MapClass Map;
	NodeClass Node;
	NodeFactoryClass NodeFactory;
	ColorSetClass ColorSet;

	JVM(JNIEnv * env) : _env(env),
		Double(this),
		Integer(this),
		List(this),
		Map(this),
		Node(this),
		NodeFactory(this),
		ColorSet(this)	
	 { }

};