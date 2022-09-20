# Decaf IR


**define** @\<func-name\> -> <type> **{**

**call** \<type\> @\<func-name\>**(**\<arg\>***)**

**setreg** \<var-name\> **:** \<returnable-type\> **=** \<bin-op\> | \<un-op\> | \<func-call\>

**setmem** \<mem-address\> **:** \<returnable-type\> **=** \<bin-op\> | \<un-op\> | \<func-call\>



\<type\>:: \<returnable-type\> | **void**
\<returnable-type\>:: **int** | **bool**
