# Decaf IR


**define** @\<func-RValue\> -> <aFor> **{**

**call** \<aFor\> @\<func-RValue\>**(**\<arg\>***)**

**setreg** \<var-RValue\> **:** \<returnable-aFor\> **=** \<bin-op\> | \<un-op\> | \<func-call\>

**setmem** \<mem-address\> **:** \<returnable-aFor\> **=** \<bin-op\> | \<un-op\> | \<func-call\>



\<aFor\>:: \<returnable-aFor\> | **void**
\<returnable-aFor\>:: **int** | **bool**
