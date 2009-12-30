#!/usr/bin/perl -w

use strict;

print "<?xml version=\"1.0\" ?>\n";
print "<luxboard>\n";
print "<version>1.1</version>\n";
print "<width>600</width>\n";
print "<height>266</height>\n";
print "<title>Sample External MapGenerator Result</title>\n";

print "<continent>\n";
print "<bonus>10</bonus>\n";
print "<country><id>4</id><polygon>404,76 405,88 402,101 394,113 381,120 366,120 354,115 340,113 331,101 327,88 323,75 324,62 332,50 342,41 353,33 367,34 379,38 390,43 400,51 </polygon><adjoining>5,6,7,11</adjoining></country>\n";
print "<country><id>5</id><polygon>341,41 342,41 332,50 324,62 323,75 311,79 301,74 291,68 286,58 283,48 284,38 293,31 295,22 302,18 310,15 318,18 325,19 327,28 332,31 </polygon><adjoining>4,6</adjoining></country>\n";
print "<country><id>6</id><polygon>378,120 382,133 374,144 364,153 354,162 340,165 327,160 317,152 308,143 304,132 296,120 301,107 307,95 316,86 327,88 331,101 340,113 354,115 </polygon><adjoining>4,8,10,11,5</adjoining></country>\n";
print "<country><id>7</id><polygon>482,76 482,92 471,105 458,114 445,121 430,121 415,123 401,117 394,113 402,101 405,88 404,76 400,51 409,43 421,40 433,36 446,36 455,45 460,56 </polygon><adjoining>4,11,12</adjoining></country>\n";
print "<country><id>8</id><polygon>402,172 402,184 399,196 386,202 375,204 364,205 355,202 347,196 343,187 337,180 339,171 344,165 340,165 354,162 364,153 374,144 382,133 394,138 401,151 </polygon><adjoining>6,9,11,10</adjoining></country>\n";
print "<country><id>9</id><polygon>351,202 347,208 344,213 338,215 335,222 329,222 321,224 316,219 310,215 304,210 301,202 306,194 309,188 315,182 320,176 329,175 337,177 337,180 343,187 347,196 </polygon><adjoining>8,10</adjoining></country>\n";
print "<country><id>10</id><polygon>337,171 337,177 329,175 320,176 315,182 309,188 306,194 301,202 293,197 284,190 279,181 279,170 282,160 282,147 290,136 302,130 304,132 308,143 317,152 327,160 340,165 </polygon><adjoining>9,6,8,15</adjoining></country>\n";
print "<country><id>11</id><polygon>458,138 455,150 447,158 437,162 428,161 420,159 414,158 407,157 401,151 394,138 382,133 386,122 394,114 394,113 401,117 415,123 430,121 445,121 458,114 </polygon><adjoining>8,7,12,13,4,6</adjoining></country>\n";
print "<country><id>12</id><polygon>543,140 540,155 533,169 519,177 505,181 491,186 477,183 462,180 452,168 447,158 455,150 458,138 458,114 445,121 458,114 470,109 481,103 492,103 506,99 516,108 525,116 </polygon><adjoining>11,7,13,3</adjoining></country>\n";
print "<country><id>13</id><polygon>459,180 458,188 457,195 451,202 445,208 436,207 429,201 425,195 418,193 423,185 423,180 423,176 421,169 424,163 428,161 437,162 447,158 455,150 447,158 452,168 </polygon><adjoining>11,12</adjoining></country>\n";
print "</continent>\n";

print "<continent>\n";
print "<bonus>6</bonus>\n";
print "<country><id>0</id><polygon>118,144 112,153 111,165 103,173 92,175 81,171 73,171 65,166 58,160 53,153 56,143 51,134 57,125 63,118 71,110 82,110 92,112 103,114 110,123 </polygon><adjoining>1,3</adjoining></country>\n";
print "<country><id>1</id><polygon>154,112 150,123 150,135 138,141 129,148 118,152 112,153 118,144 110,123 103,114 92,112 82,110 87,100 89,89 99,83 109,78 120,76 132,76 140,85 148,93 </polygon><adjoining>0,2,14,3</adjoining></country>\n";
print "<country><id>2</id><polygon>150,63 147,75 140,85 132,76 120,76 109,78 99,83 89,89 80,84 75,74 73,63 77,52 84,45 88,34 97,27 108,28 118,32 126,39 136,42 141,52 </polygon><adjoining>1,3,14</adjoining></country>\n";
print "<country><id>3</id><polygon>79,84 79,92 69,95 66,100 60,104 54,101 48,101 41,101 36,97 34,90 29,84 29,76 31,68 38,62 46,61 54,62 62,59 70,62 71,71 </polygon><adjoining>2,0,1,12</adjoining></country>\n";
print "<country><id>14</id><polygon>199,133 203,146 195,157 184,165 172,170 160,172 150,165 146,153 139,148 138,141 150,135 150,123 154,112 148,93 161,97 170,103 178,109 184,116 </polygon><adjoining>1,15,2</adjoining></country>\n";
print "<country><id>15</id><polygon>258,170 257,189 246,204 229,211 214,217 198,218 184,215 168,211 152,203 143,187 138,169 150,165 160,172 172,170 184,165 195,157 203,146 213,144 227,143 242,150 </polygon><adjoining>14,10</adjoining></country>\n";
print "</continent>\n";

print "<line><position>80,84 79,84</position></line>\n";
print "<line><position>394,113 394,114</position></line>\n";
print "<line><position>382,133 382,133</position></line>\n";
print "<line><position>340,165 340,165</position></line>\n";
print "<line><position>71,110 66,100</position></line>\n";
print "<line><position>89,89 79,92</position></line>\n";
print "<line><position>140,85 148,93</position></line>\n";
print "<line><position>311,79 316,86</position></line>\n";
print "<line><position>279,170 258,170</position></line>\n";
print "<line><position>544,140 636,97</position></line>\n";
print "<line><position>-56,140 36,97</position></line>\n";

print "</luxboard>\n";
