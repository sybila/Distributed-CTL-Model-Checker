/*
 * Copyright (C) 2012-2013 - Adam Streck
 * This file is a part of the ParSyBoNe (Parameter Synthetizer for Boolean Networks) verification tool.
 * ParSyBoNe is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3.
 * ParSyBoNe is released without any warranty. See the GNU General Public License for more details. <http://www.gnu.org/licenses/>.
 * For affiliations see <http://www.mi.fu-berlin.de/en/math/groups/dibimath> and <http://sybila.fi.muni.cz/>.
 */

#ifndef PARSYBONE_XML_HELPER_INCLUDED
#define PARSYBONE_XML_HELPER_INCLUDED

#include "../auxiliary/data_types.hpp"
#include "../auxiliary/common_functions.hpp"

#include "../rapidxml/rapidxml.hpp"
#include "../rapidxml/rapidxml_iterators.hpp"
#include "../rapidxml/rapidxml_print.hpp"
#include "../rapidxml/rapidxml_utils.hpp"

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// \brief This namespace encapsulates simple parsing functions with tests for bigger robustness.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
namespace XMLHelper {
   /**
    * Gets pointer to the descendant of the current node.
    *
    * @param[in] current_node	pointer to the ancestor of requested node
    * @param[in] node_name	string with the name of the decendant
    * @param[in] mandatory	true if absence of the node should raise exception
    *
    * @return	pointer to the descendant if sucessful
    */
   static rapidxml::xml_node<> * getChildNode(const rapidxml::xml_node<> * const current_node, const char* node_name, const bool mandatory = true) {
      rapidxml::xml_node<> * return_node = 0;
      // try to get the node
      return_node = current_node->first_node(node_name);
      if (return_node == 0 && mandatory)
            throw runtime_error("parser did not found the mandatory " + string(node_name) + " node");
      return return_node;
   }

   /**
    * Reads the value of the specified attribute and converts it to correct data type if possible.
    *
    * @param[out] requested_data	variable that will be filled with requested value
    * @param[in] current_node	pointer to the node holding requested attribute
    * @param[in] attribute_name	string with the name of the attribute
    * @param[in] mandatory	true if absence of the attribute should raise exception
    *
    * @return true if the argument was present, false otherwise
    */
   template <class returnType>
   static bool getAttribute(returnType & requested_data, const rapidxml::xml_node<> * const current_node, const char* attribute_name, const bool mandatory = true) {
      rapidxml::xml_attribute<> *temp_attr = 0;
      // Try to get the attribute
      temp_attr = current_node->first_attribute(attribute_name);
      // Check if the attribute has been required
      if (temp_attr == 0) {
         if (mandatory)
            throw runtime_error("parser did not found the mandatory attribute " + string(attribute_name));
         else
            return false;
      }
      else {
         // Try to convert attribute into requested data type
         try {
            requested_data = lexical_cast<returnType, char*>(temp_attr->value());
         } catch (bad_lexical_cast e) {
            output_streamer.output(error_str, "Error while parsing an attribute " + string(attribute_name) + ": " + string(e.what()) + ".");
            throw runtime_error("lexical_cast<" + string(typeid(returnType).name()) + ", char*>(" + string(temp_attr->value()) + ") failed");
         }
      }
      return true;
   }

   class NodeIter
   {
      rapidxml::xml_node<> * node;
      const char * tag;

   public:
      NodeIter() {
         tag = "";
         node = nullptr;
      }

      NodeIter(rapidxml::xml_node<> * parent, const char * tag) : node(parent), tag(tag) { }

      bool operator!=(const NodeIter& o) const
      {
         return node != o.node;
      }

      const rapidxml::xml_node<>* operator*() const
      {
         return node;
      }

      NodeIter& operator++()
      {
         node = node->next_sibling(tag);
         return *this;
      }
   };


   class NodesRange {
      NodeIter begin_node;

   public:
      NodesRange(const rapidxml::xml_node<> * first, const char * tag, const bool nonemtpy) {
         begin_node = {XMLHelper::getChildNode(first, tag, nonemtpy), tag};
      }

      NodeIter begin() const {
         return begin_node;
      }

      NodeIter end() const {
         return NodeIter();
      }
   };
}

#endif // PARSYBONE_XML_HELPER_INCLUDED
